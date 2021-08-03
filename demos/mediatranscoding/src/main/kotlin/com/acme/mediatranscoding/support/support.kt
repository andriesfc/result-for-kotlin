package com.acme.mediatranscoding.support

import com.acme.mediatranscoding.support.stringtemplating.eval
import resultk.Result
import resultk.resultOf
import resultk.success
import resultk.thenResultOf
import java.util.*

private const val INNER_CLASS_NAME_QUALIFIER = '$'
private const val DOT = '.'

val Class<*>.humanizedName: String get() = name.replace(INNER_CLASS_NAME_QUALIFIER, DOT)

object I8n {

    const val MESSAGES_BUNDLE = "Messages"

    fun message(key: String, vararg args: Any?) = resultOf<MissingResourceException, String> {
        val message = ResourceBundle.getBundle(MESSAGES_BUNDLE).getString(key)
        when {
            args.isEmpty() -> message
            else -> message.format(args)
        }.success()
    }


    fun eval(key: String, model: Any): Result<RuntimeException, String> {
        return message(key).thenResultOf { it.eval(model) }
    }
}