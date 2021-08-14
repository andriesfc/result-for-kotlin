package resultk.modelling.error

import resultk.modelling.i8n.I8nMessages
import resultk.modelling.i8n.messagesBundle
import resultk.modelling.i8n.required

open class LocalizedErrorMessagesProvider<in E : Error> private constructor(
    @Suppress("MemberVisibilityCanBePrivate") protected val messages: I8nMessages
) : ErrorMessagesProvider<E> {

    init {
        messages.bundle.required()
    }

    constructor(keyBundle: I8nMessages.KeyBundle) : this(messagesBundle(keyBundle))
    constructor(baseName: String) : this(messagesBundle(baseName))

    override fun getErrorMessage(error: E): String =
        buildErrorMessage(error, messageKey(error))

    override fun getDebugErrorMessage(error: E): String? {
        val messageKey = debugMessageKey(error).takeIf(messages::containsKey) ?: return null
        return buildDebugMessage(error, messageKey)
    }

    protected open fun messageKey(e: E): String = e.errorCode
    protected open fun debugMessageKey(e: E): String = "${messageKey(e)}.debug"

    protected open fun buildDebugMessage(error: E, messageKey: String): String {
        return messages.buildMessageWithBean(messageKey, error).get()
    }

    protected open fun buildErrorMessage(error: E, messageKey: String): String {
        return messages.buildMessageWithBean(messageKey, error).get()
    }

}