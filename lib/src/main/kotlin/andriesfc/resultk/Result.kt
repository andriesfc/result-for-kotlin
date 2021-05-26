package andriesfc.resultk

import java.util.*

sealed class Result<out E, out T> {
    data class Success<T>(val value: T) : Result<Nothing, T>(), Get<T> {
        override fun get(): T = value
    }

    data class Failure<E>(val error: E) : Result<E, Nothing>()
}

fun interface Get<out T> {
    fun get(): T
}

fun <E, T> T.result(): Result<E, T> = Result.Success(this)
fun <E, T> E.failure(): Result<E, T> = Result.Failure(this)

fun <E> Result<E, *>.getErrorOrNull(): E? {
    return when (this) {
        is Result.Failure -> error
        else -> null
    }
}

inline fun <E,T> T.resultOf(action:T.() -> Result<E,T>): Result<E, T> {
    return action.invoke(this)
}

inline fun <reified E, T> resultOf(action: () -> Result<E, T>): Result<E, T> {
    return try {
        action()
    } catch (e: Throwable) {
        when (e) {
            is E -> e.failure()
            else -> throw e
        }
    }
}

inline fun <E, T> Result<E, T>.getOr(mapError: (E) -> T): T {
    return when (this) {
        is Result.Failure -> mapError(error)
        is Result.Success -> value
    }
}

fun <T> Result<*, T>.get(): T {
    return getOr { e ->
        when (e) {
            is Throwable -> throw e
            else -> throw ValueNotPresentException("Expected value, but found $e instead.")
        }
    }
}

class ValueNotPresentException(message: String) : NoSuchElementException(message)

fun <T> Result<*, T>.getOrNull(): T? = getOr { null }

fun <E, T> Result<E, T>.onSuccess(process: (T) -> Unit): Result<E, T> {
    if (this is Result.Success) process(value)
    return this
}

fun <E, T> Result<E, T>.onFailure(processFailure: (E) -> T): Result<E, T> {
    if (this is Result.Failure) processFailure(error)
    return this
}

inline fun <E, T, R> Result<E, T>.fold(mapError: (E) -> R, mapValue: (T) -> R): R {
    return when (this) {
        is Result.Failure -> mapError(error)
        is Result.Success -> mapValue(value)
    }
}

fun <T> Result<*, T>.toOptional(): Optional<T> {
    return fold({ Optional.empty() }) { Optional.ofNullable(it) }
}

operator fun <T> Result<*, T>.component1(): Get<T> = when (this) {
    is Result.Failure -> Get {
        when (error) {
            is Throwable -> throw error
            else -> throw ValueNotPresentException("Expected value, but found: $error")
        }
    }
    is Result.Success -> this
}

operator fun <E> Result<E, *>.component2(): E? = fold({ it }, { null })

fun <E, T> Result<E, T>.swap(): Result<T, E> {
    return fold({ it.result() }, { it.failure() })
}

inline fun <E, T, R> Result<E, T>.map(mapValue: (T) -> R): Result<E, R> {
    return when (this) {
        is Result.Failure -> this
        is Result.Success -> mapValue(value).result()
    }
}

inline fun <E, T, R> Result<E, T>.mapFailure(mapError: (E) -> R): Result<R, T> {
    return when (this) {
        is Result.Failure -> mapError(error).failure()
        is Result.Success -> this
    }
}

fun <E, T> Result<E, T>.transpose(): Result<T, E> {
    return fold({ e -> e.result() }, { t -> t.failure() })
}

