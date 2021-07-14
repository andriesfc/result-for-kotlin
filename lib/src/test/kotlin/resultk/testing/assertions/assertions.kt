package resultk.testing.assertions

import assertk.Assert
import assertk.assertions.isInstanceOf
import assertk.assertions.support.expected
import resultk.Result
import java.io.File

fun <E> Assert<Result<E, *>>.isFailure(): Assert<E> {
    isInstanceOf(Result.Failure::class)
    return transform { actual -> (actual as Result.Failure<E>).error }
}

fun <T> Assert<Result<*, T>>.isSuccess(): Assert<T> {
    isInstanceOf(Result.Success::class)
    return transform { actual -> (actual as Result.Success<T>).value }
}

fun Assert<File>.doesNotExists() = given { actual ->
    if (!actual.exists()) return@given
    expected("File $actual to not exists at this time.")
}