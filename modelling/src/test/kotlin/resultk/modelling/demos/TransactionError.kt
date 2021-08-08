package resultk.modelling.demos

import resultk.Result
import resultk.Result.Failure
import resultk.modelling.error.DomainError
import resultk.modelling.error.LocalizedErrorMessagesProvider
import java.time.LocalDateTime
import java.time.ZoneOffset

sealed class TransactionError(override val errorCode: String) : DomainError,
    resultk.ThrowableProvider<TransactionError.TransactionException> {

    data class Forbidden(
        override val traceCode: String,
        val reasonCode: String,
        val weight: Int
    ) : TransactionError("error.transaction.forbidden")

    object StopEndOfFile : TransactionError("error.transaction.eof") {
        override val traceCode: String = "STOP"
    }

    abstract val traceCode: String
    override val debugErrorMessage: String? get() = messages.getDebugErrorMessage(this)
    override val errorMessage: String get() = messages.getErrorMessage(this)
    override fun throwing(): TransactionException = TransactionException(this)

    fun now() = LocalDateTime.now().atOffset(ZoneOffset.UTC).toString()

    companion object {
        private val messages =
            LocalizedErrorMessagesProvider<TransactionError>(
                "resultk/modelling/demos/TransactionError"
            )
    }

    class TransactionException internal constructor(e: TransactionError) :
        RuntimeException(
            e.debugErrorMessage ?: e.errorMessage
        ), resultk.FailureUnwrappingCapable<TransactionError> {

        private val failure by lazy { Failure(e) }
        override fun unwrapFailure(): Failure<out TransactionError> = failure
    }
}
