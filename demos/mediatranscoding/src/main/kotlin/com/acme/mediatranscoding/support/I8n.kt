package com.acme.mediatranscoding.support

import com.acme.mediatranscoding.support.stringtemplating.eval
import resultk.Result
import resultk.resultOf
import resultk.success
import resultk.thenResultOf
import java.util.*

object I8n {

    fun message(key: String, vararg args: Any?) = resultOf<MissingResourceException, String> {
        val message = ResourceBundle.getBundle(MESSAGES_BUNDLE).getString(key)
        when {
            args.isEmpty() -> message
            else -> message.format(args)
        }.success()
    }

    const val MESSAGES_BUNDLE = "Messages"

    fun eval(key: String, model: Any): Result<RuntimeException, String> {
        return message(key).thenResultOf { result.eval(model) }
    }
}