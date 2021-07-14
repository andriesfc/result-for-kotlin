package resultk.testing.domain.demo.acmepayments

import resultk.Result
import resultk.result
import resultk.success
import resultk.valueOrNull
import java.util.*
import java.util.ResourceBundle.getBundle

sealed class PaymentProcessorError(val code: String) : Result.Failure.ThrowableProvider<PaymentProcessorException> {

    internal val messageKey = "error.$code"

    object PaymentDeclined : PaymentProcessorError("payment_declined")
    object BlackedListenerPermanently : PaymentProcessorError("blacklisted_permanently")
    object InsufficientFunds : PaymentProcessorError("insufficient_funds")

    open fun message(): String = message(messageKey).get()
    override fun throwable() = PaymentProcessorException(this)
    override fun toString(): String = code

    class UpstreamError(
        val upstreamProvider: String,
        val upstreamErrorCode: String,
        val upstreamProviderErrorMessage: String? = null
    ) : PaymentProcessorError("upstream.$upstreamProvider.$upstreamErrorCode") {

        private val detailedMessage = buildString {
            val generalMessage = message("error.upstream").get()
            val mappedUpstreamErrorKey = "error.upstream.$upstreamProvider.$upstreamErrorCode"
            val mappedUpstreamMessage = message(mappedUpstreamErrorKey).valueOrNull()
            val upstreamDetailMessage = message(
                "error.upstream.see_details",
                upstreamProvider,
                upstreamErrorCode,
                upstreamProviderErrorMessage ?: notSuppliedByUpstreamProvider
            ).get()
            val noteThatUpstreamIsNotMapped = when (mappedUpstreamMessage) {
                null -> message(
                    "error.upstream.note.unmapped",
                    mappedUpstreamErrorKey,
                    PAYMENT_PROCESSOR_MESSAGES,
                    Locale.getDefault().displayLanguage
                ).get()
                else -> null
            }
            appendSentence(generalMessage)
            appendSentence(mappedUpstreamMessage)
            appendSentence(upstreamDetailMessage)
            appendSentence(noteThatUpstreamIsNotMapped)
        }

        override fun message(): String = detailedMessage
    }


    companion object {
        private val notSuppliedByUpstreamProvider = message("sentence_building.not_supplied_by_upstream_provider").get()
        private val punctuation = message("sentence_building.punctuation").get().toSet()
        private val fullStop = message("sentence_building.fullstop").get().first()
        private val oneSpace = message("sentence_building.onespace").get()
        private val Char.isNotPunctuation: Boolean get() = this !in punctuation
        val constants = PaymentProcessorError::class.sealedSubclasses.mapNotNull { it.objectInstance }
        internal const val PAYMENT_PROCESSOR_MESSAGES = "resultk/testing/domain/demo/acmepayments/PaymentProcessorMessages"
        internal fun message(key: String, vararg args: Any?): Result<MissingResourceException, String> {
            return result {
                getBundle(PAYMENT_PROCESSOR_MESSAGES).getString(key).run {
                    when {
                        args.isEmpty() -> this
                        else -> format(* args)
                    }
                }.success()
            }
        }

        private fun StringBuilder.appendSentence(sentence: String?) {
            if (sentence.isNullOrEmpty()) return
            if (isNotEmpty()) {
                if (last().isWhitespace()) trimEnd(Char::isWhitespace)
                if (last().isNotPunctuation) append(fullStop)
                append(oneSpace)
            }
            append(sentence.trimEnd(Char::isWhitespace))
            if (last().isNotPunctuation) append(fullStop)
        }

    }
}
