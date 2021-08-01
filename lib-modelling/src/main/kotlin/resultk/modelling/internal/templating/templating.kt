package resultk.modelling.internal.templating

import org.springframework.expression.ParserContext
import org.springframework.expression.spel.SpelParserConfiguration
import org.springframework.expression.spel.standard.SpelExpressionParser
import resultk.Result
import resultk.failure
import resultk.modelling.internal.templating.TemplatingError.MalformedTemplate
import resultk.modelling.internal.templating.TemplatingError.MissingMessagePlaceholders
import resultk.resultWithHandling
import resultk.success
import java.util.*

sealed class TemplatingError(val errorKey: String) {

    data class MissingMessagePlaceholders(val template: String, val placeholders: List<String>) :
        TemplatingError("error.templating.missingMessagePlaceholder")

    data class UnexpectedFailure(val cause: Throwable) :
        TemplatingError("error.templating.unexpectedFailure")

    data class MalformedTemplate(
        val index: Int,
        val template: String,
        val reportedVia: ExpressionResolver,
        val cause: Throwable?
    ) : TemplatingError("error.templating.malformedTemplateEncountered")
}

private const val PREFIX = "{{"
private const val SUFFIX = "}}"
private const val NOT_FOUND = -1

interface ExpressionResolver {
    fun accept(expression: String): Boolean
    fun eval(expression: String): Any?
}

sealed class ResolveExpression : ExpressionResolver {

    class ByPropertiesLookup(private val properties: Properties) : ResolveExpression() {
        override fun accept(expression: String): Boolean = properties.containsKey(expression)
        override fun eval(expression: String): Any? = properties.getProperty(expression)
    }

    class ByMapLookup(private val map: Map<String, Any?>) : ResolveExpression() {
        override fun accept(expression: String): Boolean = map.containsKey(expression)
        override fun eval(expression: String): Any? = map[expression]
    }

    class ByBeanModel(private val root: Any) : ResolveExpression() {
        private val parser = SpelExpressionParser(ByBeanModel)
        override fun accept(expression: String): Boolean = true
        override fun eval(expression: String): Any? = parser
            .parseExpression(expression, ByBeanModel)
            .getValue(root)

        companion object : SpelParserConfiguration(true, true), ParserContext {
            override fun isTemplate(): Boolean = false
            override fun getExpressionPrefix(): String = ""
            override fun getExpressionSuffix(): String = ""
        }
    }
}

private fun allocBuilder(minRequiredSize: Int) = StringBuilder((minRequiredSize * 1.6).toInt())

fun String.eval(
    resolver: ExpressionResolver,
    dest: StringBuilder = allocBuilder(length), // leave some space to grow!
): Result<TemplatingError, StringBuilder> = resultWithHandling(TemplatingError::UnexpectedFailure) {

    var i = 0
    val missing = mutableSetOf<String>()
    var err: TemplatingError? = null

    while (i < length) {
        val a = indexOf(PREFIX, i).takeUnless { it == NOT_FOUND } ?: break
        val b = indexOf(SUFFIX, a + PREFIX.length).takeUnless { it == NOT_FOUND } ?: break
        val name = substring(a + PREFIX.length, b).trim()
        if (!resolver.accept(name)) {
            missing += name
            dest.append(this, a, b + SUFFIX.length)
        } else {
            dest.append(this, i, a)
            try {
                dest.append(resolver.eval(name))
            } catch (e: Exception) {
                err = MalformedTemplate(i, this, resolver, e)
                break
            }
        }
        i = b + SUFFIX.length
    }

    if (missing.isNotEmpty()) {
        err = MissingMessagePlaceholders(this, missing.toList())
    }

    if (err == null && i < length) {
        dest.append(this, i, length)
    }

    err?.failure() ?: dest.success()

}

