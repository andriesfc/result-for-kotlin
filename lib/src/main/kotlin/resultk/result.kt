
package resultk

import resultk.Result.Failure
import resultk.Result.Failure.FailureUnwrapper
import resultk.Result.Success
import resultk.interop.ThrowingProducer
import java.util.*

//<editor-fold desc="Core Data types and special values">

/**
 * Result is an sealed type which represents a result of an operation: A result can either be a [Success], or an [Failure].
 * This type is specifically designed to provide precise functional control over errors, include the standard Kotlin
 * [Throwable] class.
 *
 * Treating exceptions in the same way as normal (non throwable) error codes is realized by applying the following logic:
 *
 * - If the caller specifies an exception as error via the `Result<E,*>` the exception is captured as per normal
 *   contract, otherwise it is thrown.
 * - If the caller calls [Result.get] and the captured [Failure.error] is an actual [kotlin.Throwable], it will be
 *   thrown, (again as is the normal Object Oriented way).
 * - If the caller calls [Result.get] and the captured [Failure.error], is not something which can be thrown, this
 *   library will wrap it as [DefaultFailureUnwrapper], and throw it.
 *
 * See the [Failure.get] function for more details on exactly how errors are handled by this `Result` implementation.
 *
 * Further more, this library provides a rich set of functions to transform/process _either_ the [Result.Success.value],
 * or in the case of a [Result.Failure] the underlying [Result.Failure.error] value. These operations can be roughly
 * be group as follows:
 *
 * - Operations to map from one type of [E], or [T] to another via the family of  _mapping_ operators.
 * - Operations to retrieve the expected success value ([T]) via a family of get operations
 * - Operations to retrieve the possible error value ([E]) via a family of get operations.
 * - Operations to take/not take an error or value based on a supplied predicate.
 * - Processing operations to transform either the success value, or the error value to another type.
 * - Terminal operations which will only be triggered in either the presence of an error or success value.
 *
 * Lastly a note on interop with the standard [kotlin.Result] type. The library provides the following
 * convenience operations to transform a [Result] to the standard [kotlin.Result] type (and visa versa):
 *
 * - [resultk.interop.toResult] to convert a [kotlin.Result] to this implementation.
 * - [resultk.interop.toStandard] to convert this result implementation to the standard `kotlin.Result` type.
 *
 * > **NOTE:** Most of the operations are implemented as extension functions.
 *
 * @param E The error type
 * @param T The successful/expected value.
 *
 * @see Failure.get
 *      For a detailed explanation of how errors are handled.
 * @see resultk.interop.toResult
 *      An extension function to convert a `kotlin.Result` to a result.
 * @see resultk.interop.toStandard
 *      An extension function to convert a `Result` to `kotlin.Result`
 */
sealed class Result<out E, out T> {

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
         * Implement this on your errors  to control which exception is raised on calling
         * [Failure.get] function.
         *
         * @param X
         *      The type throwable this failure wants use when caller tries to get an success value
         *      from this error
         */
        interface ThrowableProvider<out X : Throwable> {
            fun throwable(): X
        }

        interface FailureUnwrapper<out E> {
            fun unwrap(): Failure<out E>?
        }

        /**
         * Failures does not have an result value, only an [error]. Calling get on a failure
         * will therefore result in an exception being thrown.
         *
         * The exact exception being thrown is determined by the nature of the [error] value:
         *
         * - If the actual [error] is a [kotlin.Throwable] it will simply be thrown as expected.
         * - If the [error] implements the [Failure.ThrowableProvider] interface, the `error.throwable()` function will
         * be called to determine which throwable will be thrown.
         * - Lastly, of neither of the above applies, the error value will be wrapped a [DefaultFailureUnwrapper]
         * instance, before being thrown.
         *
         * Regardless, it is up the caller to handle, or ignore the thrown exception as usual business.
         *
         * @see DefaultFailureUnwrapper
         * @see Result.Failure.ThrowableProvider
         */
        override fun get(): Nothing {
            when (error) {
                is ThrowableProvider<Throwable> -> {
                    throw error.throwable()
                }
                is Throwable -> {
                    throw error
                }
                else -> {
                    throw DefaultFailureUnwrapper(this)
                }
            }
        }

        override fun toString(): String {
            return "${Failure::class.simpleName}($error)"
        }
    }

    /**
     * Calling get is unsafe, as the may fail (in the case of a [Failure.get]) with an exception being thrown.
     *
     * @see DefaultFailureUnwrapper.wrappedFailure - in the case where the [Failure.error] is not a throwable type.
     */
    abstract fun get(): T

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

}

private val SUCCESS_UNIT = Success(Unit)

//</editor-fold>

//<editor-fold desc="Creating results & result entry points">

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
        @Suppress("UNCHECKED_CAST")
        when (e) {
            is E -> {
                Failure(e)
            }
            is FailureUnwrapper<*> -> {
                val unwrapped = e.unwrap() ?: throw e
                unwrapped.error as? E ?: throw e
                unwrapped as Failure<E>
            }
            else -> {
                throw e
            }
        }
    }
}

//</editor-fold>

//<editor-fold desc="Accessing success values and failures">

fun <E, T> Result<E, T>.onSuccess(process: (T) -> Unit): Result<E, T> {
    (this as? Success)?.value?.also(process)
    return this
}

fun <E, T> Result<E,T>.onFailure(processFailure:(E) -> Unit) = apply {
    (this as? Failure)?.error?.also(processFailure)
}

/**
 * Returns an error or `null` for a given Result.
 *
 * @receiver A [result]
 */
fun <E> Result<E, *>.errorOrNull(): E? {
    return when (this) {
        is Failure -> error
        else -> null
    }
}

/**
 * Returns result in the first variable position - use [Result.get] to to retrieve the success value.
 *
 * > **NOTE**: If the result is a [Failure], calling [get] throw an exception.
 */
operator fun <E, T> Result<E, T>.component1(): Result<E, T> = this

/**
 * Returns the actual error if present or `null` in the second position.
 */
operator fun <E> Result<E, *>.component2(): E? = errorOrNull()

/**
 * Gets a value from a result, or maps an error to desired type. This is similar to [result.fold] operation,
 * except this only cater for mapping the error if it is present.
 */
inline fun <E, T> Result<E, T>.valueOr(mapError: (E) -> T): T {
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
inline fun <E, T, X> Result<E, T>.valueOrThrow(mapErrorToThrowable: (E) -> X): T where X : Throwable {
    return valueOr { throw mapErrorToThrowable(it) }
}

/**
 * Returns an success value, or in the case of an error a `null` value.
 *
 * @receiver This result.
 * @param T The result type.
 * @return The value of the result or null in the case of [Failure]
 */
fun <T> Result<*, T>.valueOrNull(): T? = valueOr { null }

/**
 * Returns a success value if this a success and [Success.value] matches the predicate.
 */
fun <E, T> Result<E, T>.takeSuccessIf(predicate: (T) -> Boolean): Success<T>? {
    return when {
        this is Success && predicate(value) -> this
        else -> null
    }
}

/**
 * Returns a success value if this is success and [Success.value] does not match the predicate.
 */
fun <E, T> Result<E, T>.takeSuccessUnless(predicate: (T) -> Boolean): Success<T>? {
    return when {
        this is Success && !predicate(value) -> this
        else -> null
    }
}

/**
 * Returns the error if this is an failure and the [Failure.error] matches the predicate.
 */
fun <E, T> Result<E, T>.takeFailureIf(predicate: (E) -> Boolean): Failure<E>? {
    return when {
        this is Failure && predicate(error) -> this
        else -> null
    }
}

/**
 * Returns the error if this is an failure and the [Failure.error] does not match the predicate.
 */
fun <E, T> Result<E, T>.takeFailureUnless(predicate: (E) -> Boolean): Failure<E>? {
    return when {
        this is Failure && !predicate(error) -> this
        else -> null
    }
}

//</editor-fold>

//<editor-fold desc="Mapping values and errors">
/**
 * Maps the the success value into a new type of [R], new result type.
 *
 * @param mapValue
 *      A code block which takes the current results returns a new
 */
inline fun <E, T, reified R> Result<E, T>.map(mapValue: (T) -> R): Result<E, R> {
    return when (this) {
        is Failure -> this
        is Success -> mapValue(value).success()
    }
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
inline fun <E, T, R> Result<E, T>.map(mapError: (E) -> R, mapValue: (T) -> R): R {
    return when (this) {
        is Failure -> mapError(error)
        is Success -> mapValue(value)
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
//</editor-fold>

//<editor-fold desc="Functional flow and controlled processing">
/**
 * This function produces a result from this receiver, but the caller must decide how to handle
 * any exception of type [X] being thrown from [process] code block by supplying an [caught]
 * lambda. **Note** that any exception which is not of type [X] will simply be thrown the usual way.
 *
 * @param E
 *      The value type representing the expected failure.
 * @param X
 *      The value type of the expecte exception te be thrown in the [resultFromThis] code block.
 * @param T
 *      The value type of the receiver.
 * @param R
 *      The value type of the resulting success value.
 * @param caught
 *      A lambda which takes the thrown exception of type [X] and produces a [Failure] from it.
 * @param process
 *      A code block which may either produce an [Failure] or [Success] from the receiver.
 * @return
 *      A result which will have captured either the `Failure<E>`, or a `Success<R>` value.
 */
inline fun <E, T, reified X : Throwable, R> T.resultCatching(
    caught: (X) -> E,
    process: T.() -> Result<E, R>
): Result<E, R> {
    return try {
        process()
    } catch (e: Throwable) {
        when (e) {
            is X -> caught(e).failure()
            else -> throw e
        }
    }
}

/**
 * A function which chain the processing of one result to another.
 *
 * @param
 *      process A lambda which the the [Success] as receiver and returns the next result, whether it
 *      is [Failure] or [Success]
 */
inline fun <reified E, T, R> Result<E, T>.thenResult(process: Success<T>.() -> Result<E, R>): Result<E, R> {
    return when (this) {
        is Failure -> this
        is Success -> result { process(this) }
    }
}

inline fun <E, T, reified X, R> Result<E, T>.thenResultCatching(
    caught: (e: X) -> E,
    process: Success<T>.() -> Result<E, R>
): Result<E, R> {
    return when (this) {
        is Failure -> this
        is Success -> try {
            process(this)
        } catch (e: Throwable) {
            e as? X ?: throw e
            caught(e).failure()
        }
    }
}


/**
 * Handles the function flow where exceptional flow following a result.
 *
 * @param E
 *      The failure's error type.
 * @param T
 *      The successful result value.
 * @param Er
 *      The next failure error type
 * @return
 *      A result value which contains the next error (if found).
 */
inline fun <E, T, Er> Result<E, T>.exceptOn(processFailure: Failure<E>.() -> Result<Er, T>): Result<Er, T> {
    return when (this) {
        is Success -> this
        is Failure -> processFailure()
    }
}
//</editor-fold>

//<editor-fold desc="Error code wrapping">
/**
 * The purpose of this class is to bridge the functional model of the [Result] operations with the traditional
 * _try-catch_ world of Object Oriented contract which specifies that failures should be raised and the current
 * operation should be aborted.
 *
 * @constructor Creates a new wrapped failure exception which can be raised using the `throw` operation.
 * @param wrapped The failure to wrap.
 * @see Result.get
 * @see result
 */
@Suppress("UNCHECKED_CAST")
private class DefaultFailureUnwrapper(
    wrapped: Failure<*>
) : RuntimeException("${wrapped.error}"), FailureUnwrapper<Any> {
    private val _wrapped = wrapped as Failure<Any>
    override fun unwrap(): Failure<out Any> = _wrapped
}

@Suppress("UNCHECKED_CAST")
inline fun <reified E> Throwable.unwrapFailure(): Failure<E> {
    return when {
        this is FailureUnwrapper<*> && unwrap()?.error is E -> unwrap() as Failure<E>
        else -> throw this
    }
}
//</editor-fold>

//<editor-fold desc="Conversions">
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

//</editor-fold>

