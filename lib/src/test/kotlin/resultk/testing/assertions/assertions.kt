package resultk.testing.assertions

import assertk.Assert
import assertk.assertions.isInstanceOf
import resultk.Result

fun <E> Assert<Result<E, *>>.isFailureResult(): Assert<E> {
    isInstanceOf(Result.Failure::class)
    return transform { actual -> (actual as Result.Failure<E>).error }
}

fun <T> Assert<Result<*, T>>.isSuccessResult(): Assert<T> {
    isInstanceOf(Result.Success::class)
    return transform { actual -> (actual as Result.Success<T>).value }
}