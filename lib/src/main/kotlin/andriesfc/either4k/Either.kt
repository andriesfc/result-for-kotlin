package andriesfc.either4k

import java.util.Optional
import kotlin.NoSuchElementException

sealed class Either<out E, out T> {
    data class Success<T>(val value: T) : Either<Nothing, T>()
    data class Failure<E>(val error: E) : Either<E, Nothing>()
}

fun <T> success(value: T): Either.Success<T> = Either.Success(value)
fun <E> failure(error: E): Either.Failure<E> = Either.Failure(error)

inline fun <reified E, T> either(action: () -> Either<E, T>): Either<E, T> {
    return try {
        action()
    } catch (e: Throwable) {
        when (e) {
            is E -> failure(e)
            else -> throw e
        }
    }
}

inline fun <E, T> Either<E, T>.getOr(mapError: (E) -> T): T {
    return when (this) {
        is Either.Failure -> mapError(error)
        is Either.Success -> value
    }
}

fun <T> Either<*, T>.get(): T {
    return getOr { e ->
        when (e) {
            is Throwable -> throw e
            else -> throw ValueNotPresentException("Expected value, but found $e instead.")
        }
    }
}

class ValueNotPresentException(message: String) : NoSuchElementException(message)

fun <T> Either<*, T>.getOrNull(): T? = getOr { null }

infix fun <E, T> Either<E, T>.ifPresent(process: (T) -> Unit): Either<E, T> {
    if (this is Either.Success) process(value)
    return this
}

infix fun <E, T> Either<E, T>.ifFailed(processFailure: (E) -> T): Either<E, T> {
    if (this is Either.Failure) processFailure(error)
    return this
}

inline fun <E, T, R> Either<E, T>.fold(mapError: (E) -> R, mapValue: (T) -> R): R {
    return when (this) {
        is Either.Failure -> mapError(error)
        is Either.Success -> mapValue(value)
    }
}

fun <T> Either<*, T>.toOptional(): Optional<T> {
    return fold({ Optional.empty() }) { Optional.ofNullable(it) }
}

operator fun <T> Either<*, T>.component1(): () -> T = this::get
operator fun <E> Either<E, *>.component2(): E? = fold({ it }, { null })

