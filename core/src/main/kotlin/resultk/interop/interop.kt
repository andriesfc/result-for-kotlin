package resultk.interop

import resultk.Result
import resultk.failure
import resultk.success

private typealias StdResult<T> = kotlin.Result<T>

/**
 * Converts a [kotlin.Result] to an equivalent [Result] type where the possible [Result.Failure.error] will be
 * a [kotlin.Throwable]. It up the caller to convert the exception to an appropriate error value/type.
 */
fun <T> StdResult<T>.toResult(): Result<Throwable, T> {
    return fold(
        onSuccess = { r -> r.success() },
        onFailure = { e -> e.failure() }
    )
}

/**
 * Converts a [kotlin.Result] to an equivalent [Result] type where the possible [kotlin.Result.exceptionOrNull]
 * is converted to an appropriate error value of via the supplied [throwableAsError] function block
 *
 * @param throwableAsError
 *      A function code block which is responsible to convert the captured low level kotlin.Throwable
 *      to an appropriate error value type
 * @param
 *      E The possible error type
 * @param
 *      T The expected success value type.
 *
 * @return
 *      A result value
 */
inline fun <E, T> StdResult<T>.toResult(throwableAsError: (Throwable) -> E): Result<E, T> {
    return fold(
        onSuccess = { r -> r.success() },
        onFailure = { e -> throwableAsError(e).failure() }
    )
}

fun <E, T> Result<E, T>.toStdLibResult(): StdResult<T> {
    return runCatching { get() }
}