package com.acme.mediatranscoding.support.stringtemplating

import org.springframework.expression.ParserContext
import org.springframework.expression.spel.SpelParseException
import org.springframework.expression.spel.SpelParserConfiguration
import org.springframework.expression.spel.standard.SpelExpressionParser
import resultk.Result
import resultk.resultOf
import resultk.success

private object Config : SpelParserConfiguration(
    true /* auto grow null */,
    true /* auto grow collections */
), ParserContext {
    override fun isTemplate(): Boolean = true
    override fun getExpressionPrefix(): String = "{{"
    override fun getExpressionSuffix(): String = "}}"
}

private const val NOT_FOUND = -1
private val BLANK_TEMPLATE_OK = Result.Success("")

fun String.eval(model: Any): Result<SpelParseException, String> = resultOf {

    if (isBlank()) {
        return BLANK_TEMPLATE_OK
    }

    val prefix = Config.expressionPrefix
    val suffix = Config.expressionSuffix
    val parser = SpelExpressionParser(Config)
    val out = StringBuilder()
    var i = 0

    while (i<length) {
        val a = indexOf(prefix, i).takeUnless { it == NOT_FOUND } ?: break
        val b = indexOf(suffix, i).takeUnless { it == NOT_FOUND } ?: break
        val exprString = substring(a, b + suffix.length)
        val expr = parser.parseExpression(exprString, Config)
        val t = expr.getValue(model)
        if (a > 0) out.append(this, i, a)
        out.append(t)
        i = b + suffix.length
    }

    if (i < length) {
        out.append(this, i, length)
    }

    return out.toString().success()
}