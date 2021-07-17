@file:JvmName("JavaInterop")

package resultk.interop.java

import resultk.Result
import resultk.failure
import resultk.internal.internalMessage
import resultk.success
import java.util.*

/**
 * Use this extension function to convert a standard [java.util.Optional] to a [resultk.Result] instance. The caller
 * has to to supply a code block to determine what error value of [E] should be used in case of an empty optional.
 *
 * @param E
 *      The resulting error type in case of an empty optional.
 * @param T
 *      The resulting success value type in case of non empty optional.
 * @param emptyFailureCase
 *      A code block/lambda which produces the desired result failure error value.
 */
inline fun <E, T> Optional<T>.toResult(emptyFailureCase: () -> E): Result<E, T> {
    return when (val v = orElse(null)) {
        null -> emptyFailureCase().failure()
        else -> v.success()
    }
}

/**
 * Use this extension function to convert a standard [java.util.Optional] to [resultk.Result] instance. Note that
 * an `Result.Failure<NoSuchElementException>` becomes the failure case.
 */
fun <T> Optional<T>.toResult(): Result<NoSuchElementException, T> =
    toResult { NoSuchElementException(internalMessage("error.noSuchValueInOption", this)) }

/**
 * Use this extension function to convert a result value to a [java.util.Optional] instance. As expected, any possible
 * [Result.Failure.error] value will be ignored as an Optional type only represent a value no value.
 */
fun <T> Result<*, T>.toOptional(): Optional<T> {
    return when (this) {
        is Result.Success -> Optional.ofNullable(value)
        else -> Optional.empty()
    }
}

