package resultk.modelling.internal.templating

import org.springframework.expression.ParserContext
import org.springframework.expression.spel.SpelParserConfiguration
import org.springframework.expression.spel.standard.SpelExpressionParser
import resultk.*
import resultk.modelling.internal.InternalModellingError
import resultk.modelling.internal.InternalModellingError.UnresolvedTemplateExpression
import resultk.modelling.internal.templating.ExpressionResolver.PostProcessor
import resultk.modelling.internal.templating.ExpressionResolver.PostProcessor.UnhandledExpressionProcessor.UnprocessedExpressionResolution
import java.util.*

interface ExpressionResolver {

    fun accepts(expression: String): Boolean
    fun eval(expression: String): Any?

    sealed interface PostProcessor<in T, out R> {

        fun postProcess(result: T): R

        interface UnhandledExpressionProcessor
            : PostProcessor<List<String>, UnprocessedExpressionResolution> {

            sealed class UnprocessedExpressionResolution {

                object Ignore : UnprocessedExpressionResolution()

                object IsFailure : UnprocessedExpressionResolution()

                data class FailOnlyWithThese(
                    val failedExpressions: List<String>
                ) : UnprocessedExpressionResolution() {
                    override fun describe(sb: StringBuilder) {
                        failedExpressions.joinTo(sb, prefix = "[", postfix = "]")
                    }
                }

                protected open fun describe(sb: StringBuilder) = Unit

                private val shortName: String by lazy {
                    javaClass.name
                        .replace('$', '.')
                        .split('.')
                        .takeLast(2)
                        .joinToString(".")
                }

                final override fun toString(): String {
                    return buildString {
                        append(shortName)

                        if (this@UnprocessedExpressionResolution !in singletons) {
                            append('@')
                            append(
                                this@UnprocessedExpressionResolution.hashCode().toUInt()
                                    .toString(16)
                            )
                        }

                        val emptyBodyLength = length
                        describe(this)
                        if (emptyBodyLength < length) {
                            insert(emptyBodyLength, "{ ")
                            append(" }")
                        }
                    }
                }

                companion object {
                    val singletons =
                        UnprocessedExpressionResolution::class.sealedSubclasses.mapNotNull { it.objectInstance }
                }
            }
        }

        fun interface FinalBuffer : PostProcessor<StringBuilder, Unit>
    }

}

sealed class ResolveExpression : ExpressionResolver {

    class ByPropertiesLookup(private val properties: Properties) : ResolveExpression() {
        override fun accepts(expression: String): Boolean = properties.containsKey(expression)
        override fun eval(expression: String): Any? = properties.getProperty(expression)
        override fun StringBuilder.describe() {
            append("properties: ")
            append(
                properties.entries.joinTo(
                    this,
                    transform = { (k, v) -> "$k=$v" },
                    separator = ";"
                )
            )
        }

    }

    class ByMapLookup(private val map: Map<String, Any?>) : ResolveExpression() {
        override fun accepts(expression: String): Boolean = map.containsKey(expression)
        override fun eval(expression: String): Any? = map[expression]
        override fun StringBuilder.describe() {
            append("map: ")
            append(map)
        }
    }

    class ByLookupFunction(
        private val lookup: (String) -> Any?,
        private val contains: (String) -> Boolean,
        private val resolveIgnored: (List<String>) -> UnprocessedExpressionResolution
    ) : ResolveExpression(), PostProcessor.UnhandledExpressionProcessor {

        constructor(lookup: (String) -> Any?) : this(lookup,
            contains = { true },
            resolveIgnored = { UnprocessedExpressionResolution.Ignore }
        )

        constructor(lookup: (String) -> Any?, contains: (String) -> Boolean) : this(
            contains = contains,
            lookup = lookup,
            resolveIgnored = { UnprocessedExpressionResolution.IsFailure }
        )

        override fun accepts(expression: String): Boolean = contains(expression)
        override fun eval(expression: String): Any? = lookup(expression)
        override fun postProcess(result: List<String>): UnprocessedExpressionResolution =
            resolveIgnored(result)

        override fun StringBuilder.describe() {
            listOf(
                "lookup" to lookup,
                "contains" to contains,
                "resolvedIgnored" to resolveIgnored
            ).joinTo(this, transform = { (k, v) -> "$k=$v" })
        }
    }

    class ByBeanModel(private val bean: Any) : ResolveExpression() {
        private val parser = SpelExpressionParser(ByBeanModel)
        override fun accepts(expression: String): Boolean = true
        override fun eval(expression: String): Any? = parser
            .parseExpression(expression, ByBeanModel)
            .getValue(bean)

        companion object : SpelParserConfiguration(true, true), ParserContext {
            override fun isTemplate(): Boolean = false
            override fun getExpressionPrefix(): String = ""
            override fun getExpressionSuffix(): String = ""
        }

        override fun StringBuilder.describe() {
            append("beam: ")
            append(bean)
        }
    }

    protected abstract fun StringBuilder.describe()

    final override fun toString(): String = buildString {
        append(name())
        append(": <")
        describe()
        append(">")
    }

    private fun name() = javaClass.name.split('.').last().replace('$', '.')
}

private fun allocBuilder(minRequiredSize: Int) = StringBuilder((minRequiredSize * 1.6).toInt())

private val String.isSurroundedByWhitespace: Boolean
    get() = when {
        isEmpty() -> false
        first().isWhitespace() -> true
        last().isWhitespace() -> true
        else -> false
    }

private const val PREFIX = "{{"
private const val POSTFIX = "}}"
private const val NOT_FOUND = -1

fun String.eval(
    resolver: ExpressionResolver,
    dest: StringBuilder = allocBuilder(length), // leave some space to grow!
): Result<InternalModellingError, StringBuilder> = resultOf {

    var i = 0
    val missing = mutableSetOf<String>()
    var err: InternalModellingError? = null

    while (i < length) {
        val a = indexOf(PREFIX, i).takeUnless { it == NOT_FOUND } ?: break
        val b = indexOf(POSTFIX, a + PREFIX.length).takeUnless { it == NOT_FOUND } ?: break
        val raw = substring(a + PREFIX.length, b)
        val expression = if (raw.isSurroundedByWhitespace) raw.trim() else raw
        if (!resolver.accepts(expression)) {
            dest.append(this, i, b + POSTFIX.length)
            missing += expression
        } else {
            dest.append(this, i, a)
            try {
                dest.append(resolver.eval(expression))
            } catch (e: Exception) {
                raise(InternalModellingError.MalformedTemplate(i, this, resolver, e))
            }
        }
        i = b + POSTFIX.length
    }

    if (missing.isNotEmpty()) {
        err = UnresolvedTemplateExpression(this, missing.toList())
    } else if (i < length) {
        dest.append(this, i, length)
    }

    err = resolver.postProcessIgnored(err as? UnresolvedTemplateExpression)
    resolver.postProcessFinalBuffer(dest)

    err?.failure() ?: dest.success()

}

private fun ExpressionResolver.postProcessIgnored(err: UnresolvedTemplateExpression?): UnresolvedTemplateExpression? {
    if (err == null || this !is PostProcessor.UnhandledExpressionProcessor) return err
    return when (val resolution = postProcess(err.expressions)) {
        UnprocessedExpressionResolution.IsFailure -> err
        UnprocessedExpressionResolution.Ignore -> null
        is UnprocessedExpressionResolution.FailOnlyWithThese -> UnresolvedTemplateExpression(
            template = err.template,
            expressions = resolution.failedExpressions
        )
    }
}

private fun ExpressionResolver.postProcessFinalBuffer(finalBuffer: StringBuilder) {
    if (this is PostProcessor.FinalBuffer) {
        postProcess(finalBuffer)
    }
}






