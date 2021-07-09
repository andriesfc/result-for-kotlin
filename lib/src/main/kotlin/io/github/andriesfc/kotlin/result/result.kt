@file:JvmName("ResultOperations")
package io.github.andriesfc.kotlin.result

import io.github.andriesfc.kotlin.result.Result.Failure
import io.github.andriesfc.kotlin.result.Result.Success
import io.github.andriesfc.kotlin.result.interop.ThrowingProducer
import io.github.andriesfc.kotlin.result.interop.accepting
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

        override fun toString(): String = "${Success::class.simpleName}($value)"
    }

    /**
     * A failure/error value.
     *
     * @property error The error returned as an result.
     */
    data class Failure<E>(val error: E) : Result<E, Nothing>() {


        /**
         * Implement this your own result value to indicate that any
         * failure value returned should determine which [kotlin.Throwable]
         * should be raised as not handled via default wrapped exception.
         *
         * @see Failure.get
         * @see WrappedFailureAsException.wrappedFailure
         */
        interface Throwable {
            fun throwable(): kotlin.Throwable
        }

        /**
         * Raises this failure as Exception: It is up to the caller to catch and handle the raised
         * exception.
         */
        override fun get(): Nothing {
            throw when (error) {
                is Throwable -> throw error.throwable()
                is kotlin.Throwable -> throw error
                else -> WrappedFailureAsException(this)
            }
        }

        override fun toString(): String {
            return "${Failure::class.simpleName}($error)"
        }
    }

    /**
     * Calling get is unsafe, as the may fail (in the case of a [Failure.get]) with an exception being thrown.
     *
     * @see WrappedFailureAsException.wrappedFailure - in the case where the [Failure.error] is not a throwable type.
     */
    abstract fun get(): T

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

}


/**
 * Returns an error or `null` for a given Result.
 *
 * @receiver A [result]
 */
fun <E> Result<E,*>.errorOrNull(): E? {
    return when (this) {
        is Failure -> error
        else -> null
    }
}

fun <E> Result<E,*>.errorOrEmpty(): Optional<E> {
    return when (this) {
        is Failure -> Optional.ofNullable(error)
        else -> Optional.empty()
    }
}

/**
 * Returns result in the first variable position - use [Result.get] to to retrieve the success value.
 *
 * > **NOTE**: If the result is a [Failure], calling [get] throw an exception.
 */
operator fun <T> Result<*, T>.component1(): Result<*, T> = this

/**
 * Returns the actual error if present or `null` in the second position.
 */
operator fun <E> Result<E, *>.component2(): E? = errorOrNull()


val SUCCESS_UNIT = Success(Unit)

/**
 * Wraps this value of [T] as [Success]
 */
fun <E,T> T.success(): Result<E, T> {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        Unit -> SUCCESS_UNIT as Success<T>
        else -> Success(this)
    }
}

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
            is E -> Failure(e)
            else -> throw e
        }
    }
}

/**
 * Takes computation and wraps any exception (if [E] is an [Throwable])
 * as a [Failure].
 *
 * @param producer A Java Lambda which can throw an exception.
 * @param errorClass The expected error class.
 * @throws Throwable if the caught exception is not of type [E]
 * @return A result.
 */
fun <E, T> result(errorClass: Class<E>, producer: ThrowingProducer<Result<E, T>>): Result<E, T> {
    return try {
        producer.produce()
    } catch (e: Throwable) {
        if (errorClass.isInstance(e)) {
            errorClass.cast(e).failure()
        } else {
            throw e
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
 * Do something on the [Success.value] if the receiver is a [Success]
 *
 * @param process Process the success value if present.
 * @param E The error type parameter.
 * @param T The value type parameter
 * @return This receiver.
 * @receiver A [result]
 * @return [this]
 */
fun <E, T> Result<E, T>.onSuccess(process: (T) -> Unit): Result<E, T> {
    if (this is Success) process(value)
    return this
}

/**
 * Also do something with a error if this receiver is an [Failure]
 *
 * @param E [Failure.error] value type parameter
 * @param T [Success.value] value type parameter
 * @param consume A function to which accepts a [Failure.error] value of type [E] for processing.
 * @receiver Any [Result] which may have an error value of [E]
 * @return [this]
 */
fun <E, T> Result<E, T>.onFailure(consume: (E) -> Unit): Result<E, T> {
    if (this is Failure) consume(error)
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
fun <T> Result<*, T>.optional(): Optional<T> {
    return when (this) {
        is Failure -> Optional.empty()
        is Success -> Optional.ofNullable(value)
    }
}


/**
 * Converts Java's [Optional] to a [result]. Note the caller has to supply a function which
 * supplies the missing error if there is no value present on the Optional.
 *
 * @param errorOfMissingResult A function which produces a error when the [Optional.isEmpty] returns `true`
 * @param optional The [Optional] instance to convert to a proper `Result`
 *
 * @return A [Result] which has either an value, or a error.
 */
inline fun <E, T> result(optional: Optional<T>, errorOfMissingResult: () -> E): Result<E, T> {
    return when {
        optional.isPresent -> optional.get().success()
        else -> errorOfMissingResult().failure()
    }
}

/**
 * Maps a result's success value.
 */
inline fun <E, T, reified R> Result<E, T>.map(mapValue: (T) -> R): Result<E, R> {
    return when (this) {
        is Failure -> this
        is Success -> mapValue(value).success()
    }
}

/**
 * Maps a success value to a new [Result]
 *
 * @param flatMapValue A function which takes a success value if present and maps to a new result.
 *
 */
inline fun <E,T,R> Result<E,T>.flatmap(flatMapValue:(T) -> Result<E,R>): Result<E,R> {
    return when (this) {
        is Failure -> this
        is Success -> flatMapValue(value)
    }
}

/**
 * Maps an error value to a new [Result]
 */
inline fun <E, T, R> Result<E, T>.flatmapFailure(flatMapFailure: (E) -> Result<R, T>): Result<R, T> {
    return when (this) {
        is Failure -> flatMapFailure(error)
        is Success -> this
    }
}

inline fun <E, T, Er, Tr> Result<E, T>.flatmap(flatMap: (Result<E, T>, Failure<E>?) -> Result<Er, Tr>): Result<Er, Tr> {
    return flatMap(this, this as? Failure<E>)
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
 * The purpose of this class is to bridge the functional model of the [Result] operations with the traditional
 * _try-catch_ world of Object Oriented contract which specifies that failures should be raised and the current
 * operation should be aborted.
 *
 * @constructor Creates a new wrapped failure exception which can be raised using the `throw` operation.
 * @property wrapped Reports the actual failure.
 * @param wrapped The failure to wrap.
 * @see Result.get
 * @see result
 */
@Suppress("UNCHECKED_CAST")
class WrappedFailureAsException(wrapped: Failure<*>) : RuntimeException("${wrapped.error}") {
    val wrapped: Failure<Any> = wrapped as Failure<Any>
}

/**
 * Attempts to unwrap any failure raised via a [WrappedFailureAsException]
 *
 * @see WrappedFailureAsException.wrappedFailure
 */
fun Throwable.wrappedFailure(): Optional<Failure<Any>> {
    return when (this) {
        is WrappedFailureAsException -> Optional.of(wrapped)
        else -> Optional.empty()
    }
}

fun <T> result(r: StdResult<T>):Result<Throwable, T> {
    return result { r.getOrThrow().success() }
}

fun <E,T> result(r: StdResult<T>, error:(Throwable) -> E): Result<E,T> {
    return result(r).mapFailure(error)
}

/**
 * Represents this [Failure.error] as Throwable by either returning the
 * error if error is a [Throwable], or wrapping it via the [WrappedFailureAsException]
 * class.
 *
 * @return A throwable instance representing this failure.
 */
fun Failure<*>.throwable(): Throwable {
    return (error as? Throwable) ?: WrappedFailureAsException(this)
}

/**
 * Converts this [Result] to a standard library [kotlin.Result]
 *
 * @param asThrowableOf A function which takes a failure and converts it to [Throwable] instance. The
 * default implementation simply uses the [Failure.throwable] extension function for conversion.
 * @param E The error type parameter
 * @param T The value type parameter
 *
 * @return A standard [kotlin.Result]
 *
 * @see Failure.throwable
 */
@JvmOverloads
inline fun <E, T> Result<E, T>.toStdResult(
    asThrowableOf: (Failure<E>) -> Throwable = Failure<E>::throwable
): StdResult<T> {
    return when (this) {
        is Failure -> StdResult.failure(asThrowableOf(this))
        is Success -> StdResult.success(value)
    }
}