package resultk.modelling.error

import resultk.modelling.internal.templating.ExpressionResolver
import resultk.modelling.internal.templating.ResolveExpression
import resultk.modelling.internal.templating.eval
import java.util.*

interface ErrorMessage {
    fun message(): String
    fun developerMessage(): String?
}

interface Error : ErrorMessage {
    val code: String
}

interface MessagesBuilder {
    fun message(error: Error): String
    fun internalMessage(error: Error): String?
}

fun MessagesBuilder(
    messagesResources: String,
    locale: Locale? = null,
    resolverOf: (Error) -> ExpressionResolver = ResolveExpression::ByBeanModel,
    messageKeyOf: (errorCode: String) -> String = { errorCode -> errorCode },
    internalMessageKeyOf: (errorCode: String) -> String = { errorCode -> "${errorCode}.internal" }
): MessagesBuilder = object : MessagesBuilder {

    private val rb = when (locale) {
        null -> { -> ResourceBundle.getBundle(messagesResources) }
        else -> { -> ResourceBundle.getBundle(messagesResources, locale) }
    }

    override fun message(error: Error): String {
        return rb().getString(messageKeyOf(error.code))
            .eval(resolverOf(error)).get()
            .toString()
    }

    override fun internalMessage(error: Error): String? {
        val bundle = rb()
        val messageKey = internalMessageKeyOf(error.code)
            .takeIf(bundle::containsKey)
            ?: return null
        return messageKey.let(bundle::getString).eval(resolverOf(error)).get().toString()
    }
}