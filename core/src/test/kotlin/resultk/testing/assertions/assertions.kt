package resultk.testing.assertions

import assertk.Assert
import assertk.assertions.isInstanceOf
import resultk.Result
import java.util.*

fun <E> Assert<Result<E, *>>.isFailureResult(): Assert<E> {
    isInstanceOf(Result.Failure::class)
    return transform { actual -> (actual as Result.Failure<E>).error }
}

fun <T> Assert<Result<*, T>>.isSuccessResult(): Assert<T> {
    isInstanceOf(Result.Success::class)
    return transform { actual -> (actual as Result.Success<T>).result }
}

fun Assert<ResourceBundle>.messageKeys(sorting: Comparator<String>? = null): Assert<Set<String>> {
    return transform { bundle ->
        bundle.keySet().let { keys ->
            when (sorting) {
                null -> keys.toSet()
                else -> keys.toSortedSet(sorting)
            }
        }
    }
}

inline fun <T> Assert<T>.peek(peek: (T) -> Unit): Assert<T> = apply { given(peek) }