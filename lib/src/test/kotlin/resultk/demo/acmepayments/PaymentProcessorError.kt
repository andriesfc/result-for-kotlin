package resultk.demo.acmepayments

import resultk.Result
import resultk.onSuccess
import resultk.result
import resultk.success
import java.util.*
import java.util.ResourceBundle.getBundle

sealed class PaymentProcessorError(val code: String) : Result.Failure.ThrowableProvider {

    protected val messageKey = "error.$code"

    object PaymentDeclined : PaymentProcessorError("payment_declined")
    object BlackedListenerPermanently : PaymentProcessorError("blacklisted_permanently")
    object InsufficientFunds : PaymentProcessorError("insufficient_funds")

    open fun message(): String = message(messageKey).get()
    override fun throwable(): Throwable = PaymentProcessorException(this)

    class UpstreamError(
        val upstreamProvider: String,
        val upstreamErrorCode: String,
        val upstreamProviderErrorMessage: String?
    ) : PaymentProcessorError("upstream.$upstreamProvider.$upstreamErrorCode") {

        private fun getDetailsMessageKey() = "$messageKey.details"

        override fun message(): String {
            val generalMessage = message("error.upstream").get()
            val knownUpstreamMessage = message(getDetailsMessageKey())
            return buildString {
                fun appendSentence(sentence: String) {
                    if (isNotEmpty()) {
                        if (last() != '.') append('.')
                        append(' ')
                    }
                    append(sentence)
                }
                append(generalMessage)
                knownUpstreamMessage.onSuccess(::appendSentence)
                upstreamProviderErrorMessage?.also(::appendSentence)
            }
        }
    }

    protected fun message(key: String, vararg args: Any?): Result<MissingResourceException, String> {
        return result {
            val message = getBundle(PAYMENT_PROCESSOR_MESSAGES).getString(key)
            if (args.isEmpty()) message.success() else message.format(* args).success()
        }
    }

    companion object {
        const val PAYMENT_PROCESSOR_MESSAGES = "/resultk/demo/acmepayments/PaymentProcessorMessages"
    }

}
