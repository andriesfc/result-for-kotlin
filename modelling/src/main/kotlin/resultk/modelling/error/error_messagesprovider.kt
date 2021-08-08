package resultk.modelling.error

import resultk.modelling.i8n.*
import resultk.raise
import java.util.*
import kotlin.reflect.KClass

interface ErrorMessagesProvider<in E : Error> {
    fun getErrorMessage(error: E): String
    fun getDebugErrorMessage(error: E): String?
}

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

open class EnumBasedLocalisedMessageProvider<E>(
    keyBundle: I8nMessages.KeyBundle,
    enumClass: KClass<E>
) : LocalizedErrorMessagesProvider<E>(keyBundle)
        where E : Enum<E>, E : Error {

    constructor(baseName: String, locale: Locale, enumClass: KClass<E>) : this(
        keyBundle(
            baseName,
            locale
        ), enumClass
    )

    constructor(baseName: String, enumClass: KClass<E>) : this(
        keyBundle(baseName), enumClass
    )

    private val messagesCache: Map<E, String> = mutableMapOf()
    private val debugMessageCache: Map<E, String> = mutableMapOf()

    init {
        messagesCache as MutableMap
        debugMessageCache as MutableMap
        enumClass.javaObjectType.enumConstants.forEach { e ->
            messagesCache[e] = buildErrorMessage(e, messageKey(e))
            val debugMessageKey = debugMessageKey(e)
            if (debugMessageKey in messages) {
                debugMessageCache[e] = buildDebugMessage(e, debugMessageKey)
            }
        }
    }

    final override fun getDebugErrorMessage(error: E): String? {
        return debugMessageCache[error]
    }

    final override fun getErrorMessage(error: E): String {
        return messagesCache[error] ?: raise(
            I8nError.MissingMessageKey(
                messages.bundle.baseName,
                messages.bundle.locale,
                messageKey(error)
            )
        )
    }

}