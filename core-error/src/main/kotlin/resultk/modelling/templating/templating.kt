@file:Suppress("MemberVisibilityCanBePrivate", "FunctionName")

package resultk.modelling.templating

import org.springframework.expression.ParserContext
import org.springframework.expression.spel.SpelParserConfiguration
import org.springframework.expression.spel.standard.SpelExpressionParser
import resultk.*
import resultk.internal.internalMessage
import resultk.modelling.templating.ExpressionResolver.PostProcessor
import resultk.modelling.templating.ExpressionResolver.PostProcessor.UnhandledExpression
import resultk.modelling.templating.ExpressionResolver.PostProcessor.UnhandledExpression.Resolution
import resultk.modelling.templating.ResolveExpression.*
import resultk.modelling.templating.TemplateError.UnresolvedTemplateExpression
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmName

/**
 * This error case deals with internal modelling errors. They are not supposed to
 * be passed on to public API, except as exceptions. This will ensure that all internal errors
 * are either handled or resulting in a failure.
 *
 * @property errorKey The errorKey used to construct a message with.
 * @constructor
 */
sealed class TemplateError(val errorKey: String) : ThrowableProvider<Throwable> {

    data class UnresolvedTemplateExpression(val template: String, val expressions: List<String>) :
        TemplateError("error.templating.unresolvedTemplateExpression")

    data class MalformedTemplate(
        val index: Int,
        val template: String,
        val reportedVia: Any,
        val cause: Throwable?
    ) : TemplateError("error.templating.malformedTemplateEncountered") {
        override fun throwing(): Throwable = cause ?: super.throwing()
    }

    override fun throwing(): Throwable = DefaultFailureUnwrappingException(Result.Failure(this))

    fun message(): String = internalMessage(errorKey)
        .resolve(ResolveExpression.ByModel(this))
        .map(StringBuilder::toString).get()
}

/**
 * This interface defines the resolver used to toe evaluate an expression. The resolver
 * has two parts. First it tests if the expression can be resolved, and then
 * ask the resolver to resolve the expression.
 *
 */
interface ExpressionResolver {

    fun accepts(expression: String): Boolean
    fun resolve(expression: String): Any?

    /**
     * Any expression resolver which implements one of the supported post-processing strategies gains the ability
     * to make changes after the [String.resolve] completed, but before returning. By declaring
     * it as an interface any resolver could be retrofitted by exploiting Kotlin's delegation mechanism.
     *
     * This library only supports two kinds of post processors at this time:
     *
     * 1. Post-processing the final string buffer before returning via the
     * [PostProcessor.FinalBuffer] strategy.
     * 2. Dealing with unresolved expressions via the [PostProcessor.UnhandledExpression] strategy.
     *
     * @param T The things which must be post processed.
     * @param R The result of the post processor
     * @see UnhandledExpression
     * @see PostProcessor.FinalBuffer
     */
    sealed interface PostProcessor<in T, out R> {

        fun postProcess(result: T): R

        /**
         * Use this strategy to do any cleanup of the final result.
         */
        fun interface FinalBuffer : PostProcessor<StringBuilder, Unit>

        /**
         * Implement this in your resolver to deal with any unhandled expressions.
         *
         * The result of this post processor is [Resolution] which determine how the
         * [String.resolve] returns to the caller.
         *
         * The following resolutions are available:
         *
         * - [Resolution.Ignore] will ignore any unresolved expressions, and just keep them in the final result.
         * - [Resolution.IsFailure] will result in [TemplateError.UnresolvedTemplateExpression] error.
         * - [Resolution.FailOnlyWithThese] will only result in a [TemplateError.UnresolvedTemplateExpression] error on specific missing expressions.
         */
        interface UnhandledExpression : PostProcessor<List<String>, Resolution> {

            /**
             * Specific resolution strategies for handling un resolved expressions.
             *
             * @property shortName Just a log friendly name used to describe a resolution.
             * @property isSingleton Determine if this resolution is singleton, used to describe this resolution
             */
            sealed class Resolution {

                /**
                 * Ignore, will keep the missing expressions on the final resolved string.
                 */
                object Ignore : Resolution()

                /**
                 * Make sure unresolved expressions result in `Result<String,UnresolvedTemplateExpression>` (the default behaviour)
                 */
                object IsFailure : Resolution()

                /**
                 * Supply a list of allowed failed expressions
                 * @property failedExpressions List<String>
                 * @constructor
                 */
                data class FailOnlyWithThese(
                    val failedExpressions: List<String>
                ) : Resolution() {
                    override fun describe(sb: StringBuilder) {
                        failedExpressions.joinTo(sb, prefix = "[", postfix = "]")
                    }
                }

                /**
                 * Used internally by the [toString] to describe this resolution further.
                 *
                 * @param sb StringBuilder
                 */
                protected open fun describe(sb: StringBuilder) = Unit

                private val shortName: String by lazy {
                    javaClass.name
                        .replace('$', '.')
                        .split('.')
                        .takeLast(2)
                        .joinToString(".")
                }

                private val isSingleton: Boolean get() = this in singletons

                final override fun toString(): String {

                    val hashCode = if (isSingleton) null else hashCode().toUInt().toString(16)

                    return buildString {
                        append(shortName)
                        if (hashCode != null) {
                            append('@')
                            append(hashCode)
                        }

                        val len = length
                        describe(this)
                        val described = len < length
                        if (described) {
                            insert(len, "{ ")
                            append(" }")
                        }
                    }
                }

                companion object {
                    private val singletons =
                        Resolution::class.sealedSubclasses.mapNotNull { it.objectInstance }
                }
            }
        }
    }
}


/**
 * The different kind expression resolution strategies provided out of the box.
 *
 * Currently, the following strategies are supported:
 *
 * - Supplying functions to accept and build with - [By]
 * - Supplying a [java.util.Properties] instances - [ByPropertiesLookup]
 * - Supplying a [java.util.Map] - [ByMapLookup]
 * - Supplying a bean model via spring template - [ByModel]
 * - Using introspection of an underlying bean - [ByIntrospection]
 */
sealed class ResolveExpression : ExpressionResolver {

    class By(
        private val accepting: (expression: String) -> Boolean,
        private val resolving: (String) -> Any?
    ) : ResolveExpression() {

        constructor(resolving: (String) -> Any?) : this(always, resolving)

        override fun StringBuilder.describe() {
            append("accepting:").append(accepting).append("; ")
            append("resolving").append(resolving)
        }

        override fun accepts(expression: String): Boolean = accepting(expression)
        override fun resolve(expression: String): Any? = resolving(expression)

        private companion object {
            private val always = fun(_: String) = true
        }
    }

    class ByPropertiesLookup(private val properties: Properties) : ResolveExpression() {
        override fun accepts(expression: String): Boolean = properties.containsKey(expression)
        override fun resolve(expression: String): Any? = properties.getProperty(expression)
        override fun StringBuilder.describe() {
            append("properties: ")
            properties.entries.joinTo(this, separator = ";") { (k, v) -> "$k=$v" }
        }
    }

    class ByMapLookup(private val map: Map<String, Any?>) : ResolveExpression() {
        override fun accepts(expression: String): Boolean = map.containsKey(expression)
        override fun resolve(expression: String): Any? = map[expression]
        override fun StringBuilder.describe() {
            append("map: ")
            append(map)
        }
    }

    class ByModel(private val model: Any) : ResolveExpression() {
        private val parser = SpelExpressionParser(ByModel)
        override fun accepts(expression: String): Boolean = true
        override fun resolve(expression: String): Any? = parser
            .parseExpression(expression, ByModel)
            .getValue(model)

        companion object : SpelParserConfiguration(true, true), ParserContext {
            override fun isTemplate(): Boolean = false
            override fun getExpressionPrefix(): String = ""
            override fun getExpressionSuffix(): String = ""
        }

        override fun StringBuilder.describe() {
            append("model: ")
            append(model)
        }
    }

    class ByIntrospection(private val model: Any) : ResolveExpression() {

        private val mapped =
            model::class.memberProperties.associate { p -> p.name to p.call(model) }

        override fun StringBuilder.describe() {
            append(model::class.jvmName)
            append(": {")
            mapped.keys.joinTo(this) { k -> "$k=${mapped[k]}" }
            append("}")
        }

        override fun accepts(expression: String): Boolean = expression in mapped
        override fun resolve(expression: String): Any? = mapped[expression]

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

fun String.resolve(
    resolver: ExpressionResolver,
    dest: StringBuilder = allocBuilder(length), // leave some space to grow!
): Result<TemplateError, StringBuilder> = resultOf {

    var i = 0
    val missing = mutableSetOf<String>()
    var err: TemplateError? = null

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
                dest.append(resolver.resolve(expression))
            } catch (e: Exception) {
                err = TemplateError.MalformedTemplate(i, this, resolver, e)
                break
            }
        }
        i = b + POSTFIX.length
    }

    if (missing.isNotEmpty()) {
        err = UnresolvedTemplateExpression(this, missing.toList())
    } else if (i < length) {
        dest.append(this, i, length)
    }

    (err as? UnresolvedTemplateExpression)?.also {
        err = resolver.postProcessIgnored(it)
    }

    if (err == null) {
        resolver.postProcessFinalBuffer(dest)
    }

    err?.failure() ?: dest.success()

}

private fun ExpressionResolver.postProcessIgnored(err: UnresolvedTemplateExpression): UnresolvedTemplateExpression? {
    if (this !is UnhandledExpression) {
        return err
    }
    return when (val resolution = postProcess(err.expressions)) {
        Resolution.IsFailure -> err
        Resolution.Ignore -> null
        is Resolution.FailOnlyWithThese -> UnresolvedTemplateExpression(
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






