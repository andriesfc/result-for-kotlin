package resultk.modelling.error

import resultk.modelling.i8n.I8nError
import resultk.modelling.i8n.I8nMessages
import resultk.modelling.i8n.keyBundle
import resultk.raise
import java.util.*
import kotlin.reflect.KClass

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