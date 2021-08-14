@file:JvmName("I8n")

package resultk.modelling.i8n

import resultk.*
import resultk.Result.Failure
import resultk.internal.internalMessage
import resultk.modelling.i8n.I8nError.*
import resultk.modelling.templating.ResolveExpression
import resultk.modelling.templating.TemplateError
import resultk.modelling.templating.eval
import java.util.*
import kotlin.collections.AbstractSet

/**
 * Caters for all reasonable errors when dealing with message bundles.
 *
 * There are basically 3 errors to consider when dealing with messages in resources bundles:
 *
 * 1. [MissingResourceBundle] : The resource bundle could not be found on the classpath.
 * 2. [MissingMessageKey] : The resource bundle exists, but the message key does not.
 * 3. [MessageBuildFailure] : The message resource exists, but final message could not build using
 * any of the [messagesBundle.build] functions.
 *
 * @property errorKey String The errorKey to identify which provide a friendly message
 * @property locale Locale The local of resource bundle/message in question.
 * @constructor
 */
sealed class I8nError(private val errorKey: String) {

    abstract val locale: Locale

    data class MissingResourceBundle(
        val missingBasename: String,
        override val locale: Locale
    ) : I8nError("error.i8n.missingResourceBundle"), ThrowableProvider<MissingResourceException> {
        override fun throwing(): MissingResourceException {
            return object : MissingResourceException(message(), missingBasename, null),
                FailureUnwrappingCapable<I8nError> {
                private val failure = Failure(this@MissingResourceBundle)
                override fun unwrapFailure(): Failure<out I8nError> = failure
            }
        }
    }

    data class MissingMessageKey(
        val baseName: String,
        override val locale: Locale,
        val missingKey: String,
    ) : I8nError("error.i8n.missingMessageKey"), ThrowableProvider<MissingResourceException> {
        override fun throwing(): MissingResourceException {
            return object : MissingResourceException(message(), baseName, missingKey),
                FailureUnwrappingCapable<I8nError> {
                private val failure = Failure(this@MissingMessageKey)
                override fun unwrapFailure(): Failure<out I8nError> = failure
            }
        }
    }

    data class MessageBuildFailure(
        val baseName: String,
        val templateMessageKey: String,
        override val locale: Locale,
        val template: String,
        val resolverErrorMessage: String,
        val cause: Throwable?
    ) : I8nError("error.i9n.invalidMessageTemplate")

    fun message() =
        internalMessage(errorKey)
            .eval(ResolveExpression.ByBeanModel(this))
            .get().toString()
}


/**
 * A container which can be used access (and build messages), based on resource bundles on the
 * classpath. ***Note*** the container is _read only_!
 *
 * The container behaves like `Map<String,String?>` in almost every way but with some
 * special powers:
 *
 * 1. Accessing information about the underlying bundle via the [bundle] property.
 * 2. Do extensive error checking by calling the [queryKey] function which will either return the message (as in not a `null`), or the actual error.
 * 3. Treat the underlying message as template to build rich messages in the following manner:
 *   - Build message via a "bean" - [buildMessageWithBean]
 *   - Build message via a map of key values - [buildMessageWithMap]
 *   - Build it via variable arguments of key and value pairs - [buildMessageWithKeyValues]
 */
sealed interface I8nMessages : Map<String, String?> {

    /**
     * A key bundle is bound to as specific resource with a base name and a locale. A key bundle
     * will always contain unique non `null` message keys.
     *
     * **NOTE:** To create key bundle use the [keyBundle] function.
     *
     * @property baseName
     *      String The base name.
     * @property locale
     *      The locale.
     */
    sealed interface KeyBundle : Set<String> {

        val baseName: String

        val locale: Locale

        /**
         * Determine if the key bundle for the given local exists.
         * @return Boolean
         */
        fun isAvailable(): Boolean

        /** Determine if this key bundle is empty, e.g the resource exists, but is empty. */
        override fun isEmpty(): Boolean
    }

    val bundle: KeyBundle

    /**
     * Retrieves the message immediately failing if not available. Caller has to handle the possible
     * exceptions
     *
     * @param key String
     * @return String
     */
    fun message(key: String): String = queryKey(key).get()

    fun queryKey(key: String): Result<I8nError, String>
    fun buildMessageWithBean(key: String, bean: Any): Result<I8nError, String>
    fun buildMessageWithMap(key: String, map: Map<String, Any?>): Result<I8nError, String>
    fun buildMessageWithKeyValues(
        key: String,
        pair: Pair<String, Any?>,
        vararg pairs: Pair<String, Any?>
    ): Result<I8nError, String> {
        return this.buildMessageWithMap(key, mutableMapOf<String, Any?>().apply {
            this += pair
            this += pairs
        })
    }
}

fun I8nMessages.KeyBundle.required(): I8nMessages.KeyBundle = apply {
    if (!isAvailable()) {
        raise(I8nError.MissingResourceBundle(baseName, locale))
    }
}

/**
 * Constructs i8n messages container to access and build messages from.
 *
 * @param baseName String base name from which to locate the message from.
 * @param locale Locale? The locale, or use the default locale.
 *
 * @return I8nMessages A fully realized message container.
 */
@JvmOverloads
fun messagesBundle(
    baseName: String,
    locale: Locale? = null
): I8nMessages = DefaultI8nMessages(DefaultKeyBundle(baseName, locale))

fun messagesBundle(keyBundle: I8nMessages.KeyBundle): I8nMessages = DefaultI8nMessages(keyBundle)

fun keyBundle(baseName: String, locale: Locale? = null): I8nMessages.KeyBundle {
    return DefaultKeyBundle(baseName, locale)
}

private class DefaultKeyBundle(
    override val baseName: String, locale: Locale?
) : AbstractSet<String>(), I8nMessages.KeyBundle {

    private val _locale = locale
    override val locale: Locale get() = _locale ?: Locale.getDefault()

    fun rb(): Result<MissingResourceBundle, ResourceBundle> {
        return resultWithHandlingOf({ _: MissingResourceException ->
            MissingResourceBundle(
                baseName,
                locale
            )
        }) {
            when (_locale) {
                null -> ResourceBundle.getBundle(baseName).success()
                else -> ResourceBundle.getBundle(baseName, locale).success()
            }
        }
    }

    override fun isAvailable(): Boolean = rb().isSuccess

    override val size: Int get() = rb().map { it.keySet().size }.or(0)
    override fun iterator(): Iterator<String> =
        rb().map { it.keySet().iterator() }.or { emptySet<String>().iterator() }

}

private class DefaultI8nMessages(override val bundle: I8nMessages.KeyBundle) : I8nMessages {

    override fun get(key: String): String? {
        val (m, e) = this.queryKey(key)
        return if (e == null) {
            m.get()
        } else when (e) {
            is MessageBuildFailure -> null
            is MissingMessageKey -> null
            is MissingResourceBundle -> raise(e)
        }
    }

    override fun queryKey(key: String): Result<I8nError, String> {

        val handleMissingResourceException = { _: MissingResourceException ->
            MissingMessageKey(
                bundle.baseName,
                bundle.locale,
                key
            )
        }

        return (bundle as DefaultKeyBundle).rb()
            .thenResultWithHandling(handleMissingResourceException) { rb ->
                rb.getString(key).success()
            }
    }

    override fun buildMessageWithBean(key: String, bean: Any): Result<I8nError, String> {

        val (message, failure) = this.queryKey(key)

        if (failure != null) {
            return failure.failure()
        }

        return message.thenResultOf { template ->
            template.eval(ResolveExpression.ByBeanModel(bean))
                .map(StringBuilder::toString)
                .mapError { e ->
                    MessageBuildFailure(
                        baseName = bundle.baseName,
                        templateMessageKey = key,
                        locale = bundle.locale,
                        resolverErrorMessage = e.message(),
                        template = template,
                        cause = when (e) {
                            is TemplateError.MalformedTemplate -> e.cause
                            is TemplateError.UnresolvedTemplateExpression -> null
                        }
                    )
                }
        }

    }

    override fun buildMessageWithMap(
        key: String,
        map: Map<String, Any?>
    ): Result<I8nError, String> {

        val message = queryKey(key)

        return message.thenResultOf { template ->
            template.eval(ResolveExpression.ByMapLookup(map))
                .map(StringBuilder::toString)
                .mapError { e ->
                    MessageBuildFailure(
                        baseName = bundle.baseName,
                        templateMessageKey = key,
                        locale = bundle.locale,
                        resolverErrorMessage = e.message(),
                        template = template,
                        cause = when (e) {
                            is TemplateError.MalformedTemplate -> e.cause
                            is TemplateError.UnresolvedTemplateExpression -> null
                        }
                    )
                }
        }

    }


    @Suppress("UNCHECKED_CAST")
    override val entries: Set<Map.Entry<String, String?>>
        get() = when {
            bundle.isAvailable() -> {
                bundle.asSequence().map { messageKey ->
                    Entry(
                        messageKey,
                        get(messageKey)
                    )
                }.toSet()
            }
            else -> emptySet()
        }

    override val keys: Set<String>
        get() = bundle

    override fun toString(): String {
        return buildString {
            append("[${bundle.baseName}][${bundle.locale.country}]:")
            append(" {")
            if (bundle.isNotEmpty()) entries.forEachIndexed { index, entry ->
                entry as Entry
                if (index > 0) append("; ")
                entry.appendTo(this)
            }
            append(" }")
        }
    }

    override val size: Int
        get() {
            return when {
                bundle.isAvailable() -> bundle.size
                else -> 0
            }
        }

    override val values: Collection<String?> = bundle.map(this@DefaultI8nMessages::get)

    override fun containsKey(key: String): Boolean = key in bundle
    override fun isEmpty(): Boolean = bundle.isEmpty()

    override fun containsValue(value: String?): Boolean {
        return entries.find { (_, v) -> v == value } != null
    }

    private data class Entry(
        override val key: String,
        override val value: String?
    ) : Map.Entry<String, String?> {
        override fun toString(): String = "$key -> [$value]"
        fun appendTo(dest: StringBuilder) {
            dest.append(key)
            dest.append("->[")
            dest.append(value)
            dest.append("]")
        }
    }

    companion object {
        private val emptySet = emptySet<Any?>()
    }
}