@file:JvmName("JavaInterop")

package resultk.interop.java

import resultk.Result
import resultk.internal.resourceMessage
import resultk.result
import resultk.success
import java.util.*

fun <E, T> Optional<T>.toResult(missing: () -> Result<E, T>): Result<E, T> {
    return when {
        isPresent -> get().success()
        else -> missing()
    }
}

fun <T> Optional<T>.toResult(): Result<NoSuchElementException, T> = result {
    when (val v = orElse(null)) {
        null -> throw NoSuchElementException(resourceMessage("error.no_such_value_in_option", this))
        else -> v.success()
    }
}

fun <T> Result<*, T>.toOptional(): Optional<T> {
    return when (this) {
        is Result.Success -> Optional.of(value!!)
        else -> Optional.empty()
    }
}

