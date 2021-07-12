package resultk.demo.acmepayments

import resultk.Result
import resultk.getOrNull
import resultk.result
import resultk.success
import java.util.*
import java.util.ResourceBundle.getBundle

sealed class PaymentProcessorError(val code: String) : Result.Failure.ThrowableProvider {

    internal val messageKey = "error.$code"

    object PaymentDeclined : PaymentProcessorError("payment_declined")
    object BlackedListenerPermanently : PaymentProcessorError("blacklisted_permanently")
    object InsufficientFunds : PaymentProcessorError("insufficient_funds")

    open fun message(): String = message(messageKey).get()
    override fun throwable(): Throwable = PaymentProcessorException(this)
    override fun toString(): String = code

    class UpstreamError(
        val upstreamProvider: String,
        val upstreamErrorCode: String,
        upstreamProviderErrorMessage: String? = null
    ) : PaymentProcessorError("upstream.$upstreamProvider.$upstreamErrorCode") {

        val upstreamIsMapped: Boolean
        val mappedUpstreamMessage: String?
        val upstreamProviderErrorMessage: String?

        init {
            val mappedMessage = message(messageKey)
            upstreamIsMapped = mappedMessage.isSuccess
            mappedUpstreamMessage = mappedMessage.getOrNull()
            this.upstreamProviderErrorMessage = upstreamProviderErrorMessage ?: mappedUpstreamMessage
        }

        private val fullMessage by lazy {

            val seeDetailsMessage = message(
                "error.upstream.see_details",
                upstreamProvider,
                upstreamErrorCode,
                upstreamProviderErrorMessage
            ).get()

            val generalMessage = message("error.upstream").get()

            val noteUnmapped = if (upstreamIsMapped) null else message(
                "error.upstream.note.unmapped",
                messageKey,
                PAYMENT_PROCESSOR_MESSAGES
            ).get()

            buildString {
                append(generalMessage)
                appendSentence(mappedUpstreamMessage)
                appendSentence(seeDetailsMessage)
                appendSentence(noteUnmapped)
            }
        }

        override fun message(): String = fullMessage
    }


    companion object {
        private val punctuation = message("sentence_building.punctuation").get().toSet()
        private val fullStop = message("sentence_building.fullstop").get().first()
        private val oneSpace = message("sentence_building.onespace").get()
        private val Char.isNotPunctuation: Boolean get() = this !in punctuation
        val constants = PaymentProcessorError::class.sealedSubclasses.mapNotNull { it.objectInstance }
        internal const val PAYMENT_PROCESSOR_MESSAGES = "resultk/demo/acmepayments/PaymentProcessorMessages"
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
