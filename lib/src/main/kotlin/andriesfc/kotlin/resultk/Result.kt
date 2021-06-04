@file:JvmName("ResultOperations")
package andriesfc.kotlin.resultk

import java.util.Optional
import kotlin.jvm.Throws

/**
 * Result is an sealed type which represents a result of an operation. A result can either be a [Success], or an [Failure].
 * This type is specifically designed to provide explicit and precise control over results produced by a function, or an
 * API.
 *
 * @param E The error type
 * @param T The successful/expected value.
 */
sealed class Result<out E, out T> {

    /**
     * A successful/expected value.
     *
     * @property value The value returned.
     */
    data class Success<T>(val value: T) : Result<Nothing, T>(), UnsafeGet<T> {
        override fun get(): T = value
    }

    /**
     * A failure/error value.
     *
     * @property error The error returned as an result.
     */
    data class Failure<E>(val error: E) : Result<E, Nothing>()
}

/**
 * A functional interface representing value which may throw an exception if the underlying value is a error.
 */
fun interface UnsafeGet<out T> {

    /**
     * Returns the expected value
     *
     * @throws OperationFailedException if the underlying failure result is not an exception, otherwise the underlying
     * exception is simply thrown.
     */
    @Throws(OperationFailedException::class)
    fun get(): T
}

/**
 * Wraps this value of [T] as [Result.Success]
 */
fun <E, T> T.success(): Result<E, T> = Result.Success(this)

/**
 * Wraps this value of [E] as [Result.Failure]
 */
fun <E, T> E.failure(): Result<E, T> = Result.Failure(this)

fun <E> Result<E, *>.getErrorOrNull(): E? {
    return when (this) {
        is Result.Failure -> error
        else -> null
    }
}

inline fun <T, reified E, R> T.letResult(compute: T.() -> Result<E, R>): Result<E, R> {
    return resultOf { compute() }
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
            else -> throw OperationFailedException("Failure found (instead of an result): $e.", e)
        }
    }
}

class OperationFailedException(message: String, val failure: Any?) : IllegalStateException(message)

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
    return when (this) {
        is Result.Failure -> Optional.empty()
        is Result.Success -> Optional.ofNullable(get())
    }
}

operator fun <T> Result<*, T>.component1(): UnsafeGet<T> = when (this) {
    is Result.Failure -> UnsafeGet {
        when (error) {
            is Throwable -> throw error
            else -> throw OperationFailedException("Expected success, but found error instead: $error", error)
        }
    }
    is Result.Success -> this
}

operator fun <E> Result<E, *>.component2(): E? = fold({ it }, { null })

inline fun <E, T, R> Result<E, T>.map(mapValue: (T) -> R): Result<E, R> {
    return when (this) {
        is Result.Failure -> this
        is Result.Success -> mapValue(value).success()
    }
}

inline fun <E, T, R> Result<E, T>.mapFailure(mapError: (E) -> R): Result<R, T> {
    return when (this) {
        is Result.Failure -> mapError(error).failure()
        is Result.Success -> this
    }
}

fun <E, T> Result<E, T>.transpose(): Result<T, E> {
    return when (this) {
        is Result.Failure -> error.success()
        is Result.Success -> value.failure()
    }
}

inline fun <E, T> Optional<T>.toResult(missingError: () -> E): Result<E, T> {
    return when {
        isPresent -> get().success()
        else -> missingError().failure()
    }
}