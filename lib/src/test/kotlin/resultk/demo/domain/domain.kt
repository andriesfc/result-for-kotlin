package resultk.demo.domain

import resultk.Result.Failure
import resultk.Result.Failure.FailureUnwrapper
import resultk.Result.Failure.ThrowableProvider
import java.io.IOException
import java.security.NoSuchAlgorithmException

enum class ErrorCaseEnum {
    ERROR_CASE_1,
    ERROR_CASE_2
}

enum class ErrorEnumWSelfUnwrapping : ThrowableProvider<Exception> {

    ERROR_CASE_1,

    ;

    private val _wrapped by lazy { Failure(this) }

    override fun throwable(): Exception = SimpleErrorException(_wrapped)

    sealed interface CustomFailureUnwrapper : FailureUnwrapper<ErrorEnumWSelfUnwrapping>

    private class SimpleErrorException(
        private val _wrapped: Failure<ErrorEnumWSelfUnwrapping>
    ) : Exception(_wrapped.error.name), CustomFailureUnwrapper {
        override fun unwrap(): Failure<out ErrorEnumWSelfUnwrapping> = _wrapped
    }
}


sealed class HashingError<out X : Exception> : ThrowableProvider<X> {

    abstract val cause: X

    override fun throwable(): X = cause

    data class SourceContentNotReadable(
        val source: Any,
        override val cause: IOException
    ) : HashingError<IOException>()

    data class UnsupportedAlgorithm(
        override val cause: NoSuchAlgorithmException,
        val algorithm: String
    ) : HashingError<NoSuchAlgorithmException>()

}
