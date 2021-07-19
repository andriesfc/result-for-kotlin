
package resultk

import resultk.Result.Failure
import resultk.Result.Failure.FailureUnwrappingCapable
import resultk.Result.Success
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
 *   library will wrap it as [DefaultFailureUnwrappingException], and throw it.
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
 * - [resultk.interop.toStdLibResult] to convert this result implementation to the standard `kotlin.Result` type.
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
 * @see resultk.interop.toStdLibResult
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
        fun interface ThrowableProvider<out X : Throwable> {
            fun throwable(): X
        }

        /**
         * Implement this if interface on any exception provided by the [ThrowableProvider.failure] call
         * if you want your own exception to be able to unwrap a [Failure].
         *
         * By default the library (if not provided with one), will use an internal implementation.
         *
         * @see DefaultFailureUnwrappingException
         *      The internal exception used by the library itself if non other is provided.
         * @see unwrapOrNull
         *      A function which attempts to unwrap a specific `Failure` based on desired error value type.
         * @see resultOf
         */
        interface FailureUnwrappingCapable<out E> {
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
         * - Lastly, of neither of the above applies, the error value will be wrapped a [DefaultFailureUnwrappingException]
         * instance, before being thrown.
         *
         * Regardless, it is up the caller to handle, or ignore the thrown exception as usual business.
         *
         * @see DefaultFailureUnwrappingException
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
                    throw DefaultFailureUnwrappingException(this)
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
     * @see Failure.FailureUnwrappingCapable.unwrap
     *      In the case where the [Failure.error] is not a throwable type.
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
inline fun <reified E, T> resultOf(action: () -> Result<E, T>): Result<E, T> {
    return try {
        action()
    } catch (e: Throwable) {
        e.unwrapOrNull() ?: throw e
    }
}

/**
 * Handy function to start immediately produce a [Result] from the this receiver.
 *
 * @param E
 *      The failure error type.
 * @param T
 *      The success value type
 */
inline fun <reified E, R, T> T.resultOfCatching(processThis: T.() -> Result<E, R>): Result<E, R> {
    return resultOf { processThis(this) }
}

//</editor-fold>

//<editor-fold desc="Accessing success values and failures">

fun <E, T> Result<E, T>.onSuccess(process: (T) -> Unit): Result<E, T> {
    (this as? Success)?.value?.also(process)
    return this
}

fun <E, T> Result<E, T>.onFailure(processFailure: (E) -> Unit) = apply {
    (this as? Failure)?.error?.also(processFailure)
}

/**
 * Returns an error or `null` for a given Result.
 *
 * @receiver A [resultOfCatching]
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
 * Gets a value from a result, or maps an error to desired type. This is similar to [Result.fold] operation,
 * except this only cater for mapping the error if it is present.
 */
inline fun <E, T> Result<E, T>.getOr(mapError: (E) -> T): T {
    return when (this) {
        is Failure -> mapError(error)
        is Success -> value
    }
}

fun <E, T> Result<E, T>.getOr(errorValue: T) = getOr { errorValue }

/**
 * A get operation which ensures that an exception is thrown if the result is a [resultOfCatching.Failure]. Thr caller needs
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
 * Returns an success value, or in the case of an error a `null` value.
 *
 * @receiver This result.
 * @param T The result type.
 * @return The value of the result or null in the case of [Failure]
 */
fun <T> Result<*, T>.getOrNull(): T? = getOr { null }

//</editor-fold>

//<editor-fold desc="Mapping success values and failures">
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
 * Maps a result's failure value
 */
inline fun <E, T, R> Result<E, T>.mapFailure(mapError: (E) -> R): Result<R, T> {
    return when (this) {
        is Failure -> mapError(error).failure()
        is Success -> this
    }
}

/**
 * Converts a `Result` to single value. The caller has to supply both the [onSuccess] and [onFailure] lambda
 * to cater for the either of the error or success value presence.
 *
 * @param E
 *      The failure error value type
 * @param T
 *      The success value type.
 * @param R
 *      The desired result type.
 * @return
 *  A single value of type [T]
 */
inline fun <E, T, R> Result<E, T>.fold(
    onFailure: (E) -> R,
    onSuccess: (T) -> R
): R {
    return when (this) {
        is Failure -> onFailure(error)
        is Success -> onSuccess(value)
    }
}

//</editor-fold>

//<editor-fold desc="Functional flow and controlled processing">
/**
 * This function produces a result, but the caller must decide how to handle
 * any exception of type [Ex] being thrown from [construct] code block by supplying an [caught]
 *
 * lambda. **Note** that any exception which is not of type [Ex] will simply be thrown the usual way.
 *
 * @param E
 *      The value type representing the expected failure.
 * @param Ex
 *      The value type of the expecte exception te be thrown in the [resultFromThis] code block.
 * @param T
 *      The value type of the receiver.
 * @param R
 *      The value type of the resulting success value.
 * @param caught
 *      A lambda which takes the thrown exception of type [Ex] and produces a [Failure] from it.
 * @param construct
 *      A code block which may either produce an [Failure] or [Success] from the receiver.
 * @return
 *      A result which will have captured either the `Failure<E>`, or a `Success<R>` value.
 */
inline fun <reified Ex : Throwable, reified E, R> resultOfCatching(
    caught: (Ex) -> E,
    construct: () -> Result<E, R>
): Result<E, R> {
    return try {
        resultOf(construct)
    } catch (e: Throwable) {
        when (e) {
            is Ex -> caught(e).failure()
            else -> throw e
        }
    }
}


/**
 * The preferred way of handling exceptions when chaining the result with the a following process.
 *
 * @param Ex
 *      The Exception could result in an domain error.
 * @param E
 *      The domain error type
 * @param T
 *      The target object your are processing
 * @param R
 *      The result type this `thenResultOfCatching` should produce
 * @param caught
 *      A lambda which takes in exception of type `Ex` and produce the appropriate domain erro
 * @param process
 *      A lambda which receives a success result value.
 */
inline fun <reified Ex, reified E, T, R> Result<E, T>.thenResultOfCatching(
    caught: (e: Ex) -> E,
    process: Success<T>.() -> Result<E, R>
): Result<E, R> {
    return when (this) {
        is Failure -> this
        is Success -> try {
            resultOf { process(this) }
        } catch (e: Throwable) {
            e as? Ex ?: throw e
            caught(e).failure()
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
inline fun <reified E, T, R> Result<E, T>.thenResultOf(process: Success<T>.() -> Result<E, R>): Result<E, R> {
    return when (this) {
        is Failure -> this
        is Success -> resultOf { process(this) }
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
 * @see resultOfCatching
 */
@Suppress("UNCHECKED_CAST")
private class DefaultFailureUnwrappingException(
    wrapped: Failure<*>,
) : RuntimeException("${wrapped.error}"), FailureUnwrappingCapable<Any> {
    private val _wrapped = wrapped as Failure<Any>
    override fun unwrap(): Failure<out Any> = _wrapped
}

@Suppress("UNCHECKED_CAST")
inline fun <reified E> Throwable.unwrapOrNull(): Failure<E>? {
    return when (this) {
        is E -> Failure(this)
        !is FailureUnwrappingCapable<*> -> null
        else -> unwrap()?.takeIf { it.error is E }?.let { it as Failure<E> }
    }
}
//</editor-fold>
