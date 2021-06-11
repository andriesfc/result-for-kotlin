@file:JvmName("ResultOperations")
package io.github.andriesfc.kotlin.result

import io.github.andriesfc.kotlin.interop.accepting
import io.github.andriesfc.kotlin.result.Result.Failure
import io.github.andriesfc.kotlin.result.Result.Success
import java.util.*
import java.util.function.Consumer
import kotlin.Result as StdResult

/**
 * Result is an sealed type which represents a result of an operation: A result can either be a [Success], or an [Failure].
 * This type is specifically designed to provide explicit and precise control over results produced by a function, or an
 * API.
 *
 * @param E The error type
 * @param T The successful/expected value.
 */
sealed class Result<out E, out T>  {

    /**
     * A successful/expected value.
     *
     * @property value The value returned.
     */
    data class Success<T>(val value: T) : Result<Nothing, T>() {

        /**
         * Returns this [value]
         */
        override fun get(): T = value
    }

    /**
     * A failure/error value.
     *
     * @property error The error returned as an result.
     */
    data class Failure<E>(val error: E) : Result<E, Nothing>() {

        /**
         * Raises this failure as Exception: It is up to the caller to catch and handle the raised
         * exception.
         */
        override fun get(): Nothing {
            throw when (error) {
                is Throwable -> error
                else -> WrappedFailureAsException(this)
            }
        }
    }

    /**
     * Calling get is unsafe, as the may fail (in the case of a [Failure.get]) with an exception being thrown.
     *
     * @see WrappedFailureAsException.wrapped - in the case where the [Failure.error] is not a throwable type.
     */
    abstract fun get(): T
}

val Result<*, *>.isSuccess: Boolean get() = this is Success
val Result<*, *>.isFailure: Boolean get() = this is Failure

/**
 * Returns a getter for the of the successful result value in the first position, or
 * an getter which will thrown in exception in the case of error.
 *
 * > **NOTE**: If the result is a [Failure], calling [get] may result in an exception being thrown.
 */
operator fun <T> Result<*, T>.component1(): Result<*, T> = this

/**
 * Returns the actual error if present or `null` in the second position.
 */
operator fun <E> Result<E, *>.component2(): E? = getErrorOrNull()

/**
 * Wraps this value of [T] as [Success]
 */
fun <E, T> T.success(): Result<E, T> = Success(this)

/**
 * Wraps this value of [E] as [Failure]
 */
fun <E, T> E.failure(): Result<E, T> = Failure(this)


/**
 * Returns an error or `null` for a given Result.
 *
 * @receiver A [result]
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
inline fun <T, reified E, R> T.resultWith(compute: T.() -> Result<E, R>): Result<E, R> {
    return result { compute() }
}


/**
 * Takes computation and wraps any exception (if [E] is an [Throwable])
 * as a [Failure].
 *
 * @param action Action to produce an [Result]
 * @throws Throwable if the caught exception is not of type [E]
 * @return A result.
 */
inline fun <reified E, T> result(action: () -> Result<E, T>): Result<E, T> {
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
 * Takes computation and wraps any exception (if [E] is an [Throwable])
 * as a [Failure].
 *
 * @param action Action to produce an [result]
 * @param errorClass The expected error class.
 * @throws Throwable if the caught exception is not of type [E]
 * @return A result.
 */
fun <E, T> result(errorClass: Class<E>, action: () -> Result<E, T>): Result<E, T> {
    return try {
        action()
    } catch (e: Throwable) {
        when {
            errorClass.isInstance(e) -> errorClass.cast(e).failure()
            else -> throw e
        }
    }
}

/**
 * Gets a value from a result, or maps an error to desired type. This is similar to [result.fold] operation,
 * except this only cater for mapping the error if it is present.
 */
inline fun <E, T> Result<E, T>.getOr(mapError: (E) -> T): T {
    return when (this) {
        is Failure -> mapError(error)
        is Success -> value
    }
}

/**
 * A get operation which ensures that an exception is thrown if the result is a [result.Failure]. Thr caller needs
 * to supply a function to map the error value to the correct instance of [X
 *
 * @param mapErrorToThrowable A function which converts an error instance to an exception of type [X]
 * @param E The error type
 * @param T The successful/expected value type.
 * @param X The exception type which needs to be thrown when this result contains an error.
 * @receiver A result container which may hold either a value, or an error.
 * @return The value of the result, or throws the exception produced by the [mapErrorToThrowable] function.
 */
inline fun <E, T, X> Result<E, T>.getOrThrow(mapErrorToThrowable: (E) -> X): T where X : Throwable {
    return getOr { throw mapErrorToThrowable(it) }
}

/**
 * Returns the value of this result contains an success value, or throws an an exception if the success value is not present.
 *
 * **NOTE:**  The actual exception being thrown depends on the type of the [Failure.error] value.
 * If the error is an throwable, it will throw it, otherwise wrap into a [WrappedFailureAsException]
 *
 * If this is not the desired behaviour use any of the following operations:
 *
 * - Supplying a mapping function.
 * - Map the failure first via the [result.mapFailure] followed by [result.get]
 *
 * @see [Failure.get]
 */
fun <T> Result<*, T>.getOrThrow(): T = get()

/**
 * Returns an success value, or in the case of an error a `null` value.
 *
 * @receiver This result.
 * @param T The result type.
 * @return The value of the result or null in the case of [result.Failure]
 */
fun <T> Result<*, T>.getOrNull(): T? = getOr { null }


/**
 * Do also something on the [Success.value] if the receiver is a [Success]
 *
 * @param process Process the success value if present.
 * @param E The error type.
 * @param T The value type
 * @return This receiver.
 * @receiver A [result]
 */
fun <E, T> Result<E, T>.onSuccess(process: (T) -> Unit): Result<E, T> {
    if (this is Success) process(value)
    return this
}

/**
 * Also do something with a error if this receiver is an [Failure]
 */
fun <E, T> Result<E, T>.onFailure(processFailure: (E) -> Unit): Result<E, T> {
    if (this is Failure) processFailure(error)
    return this
}

fun <E, T> Result<E, T>.onFailure(consumer: Consumer<E>): Result<E, T> = onFailure(consumer.accepting())
fun <E, T> Result<E, T>.onSuccess(consumer: Consumer<T>): Result<E, T> = onSuccess(consumer.accepting())



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
 * Converts a [result] to an plain old Java [Optional]
 */
fun <T> Result<*, T>.toOptional(): Optional<T> {
    return when (this) {
        is Failure -> Optional.empty()
        is Success -> Optional.ofNullable(get())
    }
}


/**
 * Converts Java's [Optional] to a [result]. Note the caller has to supply a function which
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

/**
 * Casts thia result to specific type result of either the [E], or [T] value type.
 *
 * @param E The error type parameter
 * @param T The result type parameter
 * @return This result if either error value is of type [E], or the value of type [T], or `null`.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified E,reified T> Result<*, *>.castAsOrNull(): Result<E, T>? {
    return when (this) {
        is Failure -> (error as? E)?.let { this as Failure<E> }
        is Success -> (value as? T)?.let { this as Success<T> }
    }
}

/**
 * Casts this result to specific type result of either the error ([E]), or success value ([T]) type.
 *
 * @param E The error type parameter
 * @param errorClass The class of expected error value type
 * @param T The result type parameter
 * @param valueClass The class of the expected value type.
 * @return This result if either error value is of type [E], or the value of type [T], or `null`.
 */
@Suppress("UNCHECKED_CAST")
fun <E, T> Result<*, *>.castAsOrNull(errorClass: Class<out E>, valueClass: Class<out T>): Result<E, T>? {
    return when  {
        this is Failure && errorClass.isInstance(error) -> this as Failure<E>
        this is Success && valueClass.isInstance(value) -> this as Success<T>
        else -> null
    }
}

/**
 * Attempts to cast a failure of to specific error value type, or failing returns a null.
 *
 * @param E The expected error value class.
 *
 * @return The failure with the expected value type of [E], or null.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified E> Failure<*>.castAsOrNull(): Failure<E>? {
    return when (error) {
        is E -> this as Failure<E>
        else -> null
    }
}


/**
 * Attempts to cast a failure of to specific error value type, or failing returns a null.
 *
 * @param E The expected error value class.
 * @param errorClass The class of the expected error value.
 *
 * @return The failure with the expected value type of [E], or null.
 */
@Suppress("UNCHECKED_CAST")
fun <E> Failure<*>.castAsOrNull(errorClass: Class<out E>): Failure<E>? {
    return when {
        errorClass.isInstance(error) -> this as Failure<E>
        else -> null
    }
}

/**
 * Attempts to cast this success to value of a specific type of [T]. If the cast cannot
 * succeed, a `null` value will be returned.
 *
 * @param T The expected [Success.value] type parameter.
 *
 * @return A Success which value is of type [T], or `null` of the cast will cannot succeed.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> Success<*>.castAsOrNull(): Success<T>? {
    return when (value) {
        is T -> this as Success<T>
        else -> null
    }
}

/**
 * Attempts to cast this success to value of a specific type of [T]. If the cast cannot
 * succeed, a `null` value will be returned.
 *
 * @param T The expected [Success.value] type parameter.
 * @param valueClass The expected value class of [T]
 * @return A Success which value is of type [T], or `null` of the cast will cannot succeed.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Success<*>.castAsOrNull(valueClass: Class<out T>): Success<T>? {
    return when {
        valueClass.isInstance(value) -> this as Success<T>
        else -> null
    }
}

/**
 * Indicates that an error occurred, but the [Failure.error] itself could not be thrown as it
 * is not of a `Throwable` type.
 *
 * @property wrapped Reports the actual unhandled failure.
 *
 * @aee Result.get
 * @see Failure
 * @see result
 */
class WrappedFailureAsException(val wrapped: Failure<*>) : RuntimeException("Unhandled error raised: ${wrapped.error}")

/**
 * Attempts to unwrap any failure raised via a [WrappedFailureAsException]
 *
 * @see WrappedFailureAsException.wrapped
 */
fun Throwable.unwrapFailure(): Optional<Failure<Any>> {
    return when (this) {
        is WrappedFailureAsException -> wrapped.castAsOrNull<Any>()?.let { Optional.ofNullable(it) }
            ?: Optional.empty()
        else -> Optional.empty()
    }
}

/**
 * Converts a [kotlin.Result] value to [Result]
 */
fun <T> StdResult<T>.result(): Result<Throwable, T> {
    return result { getOrThrow().success() }
}

/**
 * Converts this [Result] to a standard library [kotlin.Result]
 *
 * @param failureAsThrowable A function which takes a failure an convert it to [Throwable] instance.
 * @param E The error type parameter
 * @param T The value type parameter
 *
 * @return A standard [kotlin.Result]
 */
@JvmOverloads
inline fun <E, T> Result<E, T>.toStdResult(
    failureAsThrowable: (Failure<E>) -> Throwable = { f ->
        when (f.error) {
            is Throwable -> f.error
            else -> WrappedFailureAsException(f)
        }
    }
): kotlin.Result<T> {
    return when (this) {
        is Failure -> StdResult.failure(failureAsThrowable(this))
        is Success -> StdResult.success(value)
    }
}
