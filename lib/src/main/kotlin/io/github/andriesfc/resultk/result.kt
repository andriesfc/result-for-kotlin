@file:JvmName("ResultOperations")
package io.github.andriesfc.resultk

import java.util.Optional
import kotlin.jvm.Throws
import io.github.andriesfc.resultk.Result.Failure
import io.github.andriesfc.resultk.Result.Success

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
     * Indicates that an error occurred, but cannot be represented as Exception.
     *
     * @see UnsafeGet.get
     * @see Result.Failure
     * @see resultOf
     */
    class UnsafeGetFailedException(message: String, val failure: Any?) : IllegalStateException(message)


    /**
     * Returns the expected value
     *
     * @throws UnsafeGetFailedException if the underlying failure result is not an exception, otherwise the underlying
     * exception is simply thrown.
     */
    @Throws(UnsafeGetFailedException::class)
    fun get(): T
}

/**
 * Returns a getter for the of the successful result value in the first position, or
 * an getter which will thrown in exception in the case of error.
 *
 * > **NOTE**: If the result is a [Result.Failure], calling [UnsafeGet.get] will
 * > throw an exception.
 *
 * @see UnsafeGet.get
 * @see UnsafeGet.UnsafeGetFailedException
 */
operator fun <T> Result<*, T>.component1(): UnsafeGet<T> = when (this) {
    is Failure -> UnsafeGet {
        when (error) {
            is Throwable -> throw error
            else -> throw UnsafeGet.UnsafeGetFailedException("Expected success, but found error instead: $error", error)
        }
    }
    is Success -> this
}

/**
 * Returns the actual error if present or `null` in the second position.
 */
operator fun <E> Result<E, *>.component2(): E? = fold({ it }, { null })

/**
 * Wraps this value of [T] as [Result.Success]
 */
fun <E, T> T.success(): Result<E, T> = Success(this)

/**
 * Wraps this value of [E] as [Result.Failure]
 */
fun <E, T> E.failure(): Result<E, T> = Failure(this)


/**
 * Returns an error or `null` for a given Result.
 *
 * @receiver A [Result]
 */
fun <E> Result<E, *>.getErrorOrNull(): E? {
    return when (this) {
        is Failure -> error
        else -> null
    }
}

/**
 * Lets compute a result om a given receiver from lambda.
 *
 * @receiver A value to compute a result.
 * @param compute The function to apply on given receiver.
 */
inline fun <T, reified E, R> T.computeResultWith(compute: T.() -> Result<E, R>): Result<E, R> {
    return resultOf { compute() }
}


/**
 * Takes computation and wraps any exception (if [E] is an [Throwable])
 * as a [Result.Failure].
 *
 * @param action Action to produce an [Result]
 * @throws Throwable if the caught exception is not of type [E]
 * @return A result.
 */
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


/**
 * Gets a value from a result, or maps an error to desired type. This is similar to [Result.fold] operation,
 * except this only cater for mapping the error if it is present.
 */
inline fun <E, T> Result<E, T>.getOr(mapError: (E) -> T): T {
    return when (this) {
        is Failure -> mapError(error)
        is Success -> value
    }
}

/**
 * Returns the value or throws an exception.
 *
 * @see UnsafeGet.UnsafeGetFailedException
 * @see Result.Failure
 */
fun <T> Result<*, T>.get(): T {
    return getOr { e ->
        when (e) {
            is Throwable -> throw e
            else -> throw UnsafeGet.UnsafeGetFailedException("Failure found (instead of an result): $e.", e)
        }
    }
}

/**
 * Returns an success value, or in the case of an error a `null` value.
 *
 * @receiver This result.
 * @param T The result type.
 * @return The value of the result or null in the case of [Result.Failure]
 */
fun <T> Result<*, T>.getOrNull(): T? = getOr { null }


/**
 * Do also something on the [Success.value] if the receiver is a [Success]
 *
 * @param process Process the success value if present.
 * @param E The error type.
 * @param T The value type
 * @return This receiver.
 * @receiver A [Result]
 */
fun <E, T> Result<E, T>.alsoOn(process: (T) -> Unit): Result<E, T> {
    if (this is Success) process(value)
    return this
}

/**
 * Also do something with a error if this receiver is an [Failure]
 */
fun <E, T> Result<E, T>.alsoOnFailure(processFailure: (E) -> Unit): Result<E, T> {
    if (this is Failure) processFailure(error)
    return this
}


/**
 * Folds this result to a single value. The caller has to supply both a [mapError], and [mapValue]
 * function to translate the given value or error.
 *
 * @param mapError Function to map an error value to the desired result.
 * @param mapValue Function to map an value to to desired result.
 * @param E The error type.
 * @param T The of expected value to fold.
 * @param R The desired resulting type of the fold operations.
 * @return The desired value of the fold operation, which is either an value mapped to it, or an error mapped to it.
 */
inline fun <E, T, R> Result<E, T>.fold(mapError: (E) -> R, mapValue: (T) -> R): R {
    return when (this) {
        is Failure -> mapError(error)
        is Success -> mapValue(value)
    }
}

/**
 * Converts a [Result] to an plain old Java [Optional]
 */
fun <T> Result<*, T>.toOptional(): Optional<T> {
    return when (this) {
        is Failure -> Optional.empty()
        is Success -> Optional.ofNullable(get())
    }
}


/**
 * Converts Java's [Optional] to a [Result]. Note the caller has to supply a function which
 * supplies the missing error if there is no value present on the Optional.
 */
inline fun <E, T> Optional<T>.toResult(missingError: () -> E): Result<E, T> {
    return when {
        isPresent -> get().success()
        else -> missingError().failure()
    }
}

/**
 * Maps a result's success value.
 */
inline fun <E, T, R> Result<E, T>.map(mapValue: (T) -> R): Result<E, R> {
    return when (this) {
        is Failure -> this
        is Success -> mapValue(value).success()
    }
}

/**
 * Maps a result's failure value
 */
inline fun <E, T, R> Result<E, T>.mapFailure(mapError: (E) -> R): Result<R, T> {
    return when (this) {
        is Failure -> mapError(error).failure()
        is Success -> this
    }
}

/**
 * Transposes a results success and failure values.
 */
fun <E, T> Result<E, T>.transpose(): Result<T, E> {
    return when (this) {
        is Failure -> error.success()
        is Success -> value.failure()
    }
}
