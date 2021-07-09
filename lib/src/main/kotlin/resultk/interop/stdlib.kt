package resultk.interop

import resultk.Result
import resultk.failure
import resultk.success

private typealias StandardResult<T> = kotlin.Result<T>

fun <T> StandardResult<T>.toResult(): Result<Throwable, T> {
    return fold(
        onSuccess = { r -> r.success() },
        onFailure = { e -> e.failure() }
    )
}

inline fun <E, T> StandardResult<T>.toResult(throwableAsError: (Throwable) -> E): Result<E, T> {
    return fold(
        onSuccess = { r -> r.success() },
        onFailure = { e -> throwableAsError(e).failure() }
    )
}

fun <E, T> Result<E, T>.toCompatible(): StandardResult<T> {
    return runCatching { get() }
}