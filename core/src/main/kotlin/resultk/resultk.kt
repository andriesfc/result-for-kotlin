package resultk

import resultk.Result.Failure
import resultk.Result.Success

//<editor-fold desc="Core Data types and special values">

/**
 * Result is a sealed type which represents a result of an operation: A result can either be a [Success], or an [Failure].
 * This type is specifically designed to provide precise functional control over errors.
 *
 * Treating exceptions in the same way as normal (non throwable) error codes is realized by applying the following logic:
 *
 * - If the caller specifies an exception as error via the `Result<E,*>` the exception is captured as per normal
 *   contract, otherwise it is thrown.
 * - If the caller calls [Result.get] and the captured [Failure.error] is an actual [kotlin.Throwable], it will be
 *   thrown, (again as is the normal Object-Oriented way).
 * - If the caller calls [Result.get] and the captured [Failure.error], is not something which can be thrown, this
 *   library will wrap it as [DefaultFailureUnwrappingException], and throw it.
 *
 * See the [Failure.get] function for more details on exactly how errors are handled by this `Result` implementation.
 *
 * Furthermore, this library provides a rich set of functions to transform/process _either_ the [Result.Success.result],
 * or in the case of a [Result.Failure] the underlying [Result.Failure.error] value. These operations can be roughly
 * be grouped as follows:
 *
 * - Operations to map from one type of [E], or [T] to another via the family of  _mapping_ operators.
 * - Operations to retrieve the expected success value ([T]) via a family of get operations
 * - Operations to retrieve the possible error value ([E]) via a family of get operations.
 * - Processing operations to transform either the success value, or the error value to another type.
 * - Conditional operations which will only be triggered in either the presence of an error or success value.
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
     * @property result The value returned.
     */
    data class Success<T>(val result: T) : Result<Nothing, T>() {

        /**
         * Returns this [result]
         */
        override fun get(): T = result

        override fun toString(): String = "${Success::class.simpleName}($result)"

        val value: T get() = get()
    }

    /**
     * A failure/error value.
     *
     * @property error The error returned as a result.
     */
    data class Failure<E>(val error: E) : Result<E, Nothing>() {

        /**
         * Failures does not have a result value, only an [error]. Calling get on a failure
         * will therefore result in an exception being thrown.
         *
         * The exact exception being thrown is determined by the nature of the [error] value:
         *
         * - If the actual [error] is a [kotlin.Throwable] it will simply be thrown as expected.
         * - If the [error] implements the [ThrowableProvider] interface, the `error.throwable()` function will
         * be called to determine which throwable will be thrown.
         * - Lastly, of neither of the above applies, the error value will be wrapped a [DefaultFailureUnwrappingException]
         * instance, before being thrown.
         *
         * Regardless, it is up the caller to handle, or ignore the thrown exception as usual business.
         *
         * @see DefaultFailureUnwrappingException
         * @see ThrowableProvider
         */
        override fun get(): Nothing {
            when (error) {
                is ThrowableProvider<Throwable> -> {
                    throw error.throwing()
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
fun <E, T> T.success(): Result<E, T> {
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
 * Handy function to start immediately produce a [Result] from this receiver.
 *
 * @param E
 *      The failure error type.
 * @param T
 *      The success value type
 */
inline fun <reified E, R, T> T.resultWithHandlingOf(processThis: T.() -> Result<E, R>): Result<E, R> {
    return resultOf { processThis(this) }
}

//</editor-fold>

//<editor-fold desc="Accessing success values and failures">

inline fun <E, T> Result<E, T>.onSuccess(process: (T) -> Unit): Result<E, T> {
    (this as? Success)?.result?.also(process)
    return this
}

inline fun <E, T> Result<E, T>.onError(processFailure: (E) -> Unit) = apply {
    (this as? Failure)?.error?.also(processFailure)
}

inline fun <reified E, reified Ec : E, T> Result<E, T>.onSpecificFailure(acceptError: (Ec) -> Unit): Result<E, T> {
    return onError { e: E ->
        (e as? Ec)?.also(acceptError)
    }
}

/**
 * Returns an error or `null` for a given Result.
 *
 * @receiver A [resultWithHandlingOf]
 */
fun <E> Result<E, *>.errorOrNull(): E? {
    return when (this) {
        is Failure -> error
        else -> null
    }
}

/**
 * Returns result in the first variable position - use [Result.get] to retrieve the success value.
 *
 * > **NOTE**: If the result is a [Failure], calling [get] throw an exception.
 */
operator fun <E, T> Result<E, T>.component1(): Result<E, T> = this

/**
 * Returns the actual error if present or `null` in the second position.
 */
operator fun <E> Result<E, *>.component2() = errorOrNull()

/**
 * Gets a value from a result, or maps an error to desired type. This is similar to [Result.fold] operation,
 * except this only cater for mapping the error if it is present.
 */
inline fun <E, T> Result<E, T>.or(mapError: (E) -> T): T {
    return when (this) {
        is Failure -> mapError(error)
        is Success -> result
    }
}

fun <E, T> Result<E, T>.or(errorValue: T) = or { errorValue }

/**
 * A get operation which ensures that an exception is thrown if the result is a [resultWithHandlingOf]. Thr caller needs
 * to supply a function to map the error value to the correct instance of [X
 *
 * @param mapErrorToThrowable A function which converts an error instance into an exception to type [X]
 * @param E The error type
 * @param T The successful/expected value type.
 * @param X The exception type which needs to be thrown when this result contains an error.
 * @receiver A result container which may hold either a value, or an error.
 * @return The value of the result, or throws the exception produced by the [mapErrorToThrowable] function.
 */
inline fun <E, T, X> Result<E, T>.orThrow(mapErrorToThrowable: (E) -> X): T where X : Throwable {
    return or { throw mapErrorToThrowable(it) }
}

/**
 * Returns a success value, or in the case of an error a `null` value.
 *
 * @receiver This result.
 * @param T The result type.
 * @return The value of the result or null in the case of [Failure]
 */
fun <T> Result<*, T>.orNull(): T? = or { null }


/**
 * Just get any value, even it is an error!
 *
 * @receiver Result<*, *>
 * @return Any
 */
fun Result<*, *>.any(): Any = when (this) {
    is Failure -> error as Any
    is Success -> result as Any
}

val <T> Result<*,T>.value:T get() = get()

//</editor-fold>

//<editor-fold desc="Mapping success values and failures">
/**
 * Maps the success value into a new type of [R], new result type.
 *
 * @param mapValue
 *      A code block which takes the current results returns a new
 */
inline fun <E, T, reified R> Result<E, T>.map(mapValue: (T) -> R): Result<E, R> {
    return when (this) {
        is Failure -> this
        is Success -> mapValue(result).success()
    }
}

/**
 * Maps a result's failure value
 */
inline fun <E, T, R> Result<E, T>.mapError(mapError: (E) -> R): Result<R, T> {
    return when (this) {
        is Failure -> mapError(error).failure()
        is Success -> this
    }
}

/**
 * Converts a `Result` to single value. The caller has to supply both the [onSuccess] and [onFailure] lambda
 * to cater for both possible cases
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
        is Success -> onSuccess(result)
    }
}

//</editor-fold>

//<editor-fold desc="Functional flow and controlled processing">

/**
 * This function produces a result, but the caller must decide how to handle
 * any exception being thrown from [construct] code block by supplying an [caught] lambda
 *
 * lambda. **Note** that any exception which is not of type [Ex] will simply be thrown the usual way.
 *
 * @param E
 *      The value type representing the expected failure.
 * @param Ex
 *      The type of exception the caller expects to be thrown by the [resultFromThis] code block.
 * @param R
 *      The value type of the resulting success value.
 * @param caught
 *      A lambda which takes the thrown exception and produces a [Failure] from it.
 * @param construct
 *      A code block which may either produce a [Failure] or [Success] from the receiver.
 * @return
 *      A result which will have captured either the `Failure<E>`, or a `Success<R>` value.
 */
inline fun <reified Ex : Throwable, reified E, R> resultWithHandlingOf(
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
 * This raised anything as error code via an exception.
 *
 * @receiver E
 *      The thing you want to raise as failure
 * @return Nothing
 */
inline fun <reified E> raise(e: E): Nothing {
    when (e) {
        is Throwable -> throw e
        is ThrowableProvider<Throwable> -> throw e.throwing()
        is Failure<*> -> throw DefaultFailureUnwrappingException(e)
        else -> throw DefaultFailureUnwrappingException(Failure(e))
    }
}

/**
 * The preferred way of handling exceptions when chaining the result with a following process which
 * needs to handle a different kind of exception.
 *
 * @param Ex
 *      The Exception could result in a domain error.
 * @param E
 *      The domain error type
 * @param T
 *      The target object you are processing
 * @param R
 *      The result type this `thenResultOfCatching` should produce
 * @param caught
 *      A lambda which accepts an Exception  as parameter and produces the appropriate domain error
 * @param process
 *      A lambda which receives a success result value.
 */
inline fun <reified Ex, reified E, T, R> Result<E, T>.thenResultWithHandling(
    caught: (e: Ex) -> E,
    process: (T) -> Result<E, R>
): Result<E, R> {
    return when (this) {
        is Failure -> this
        is Success -> try {
            resultOf { process(result) }
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
 *      process A lambda which the [Success] as receiver and returns the next result, whether it
 *      is [Failure] or [Success]
 */
inline fun <reified E, T, R> Result<E, T>.thenResultOf(process: (T) -> Result<E, R>): Result<E, R> {
    return when (this) {
        is Failure -> this
        is Success -> resultOf { process(result) }
    }
}


//</editor-fold>


//<editor-fold desc="Error code wrapping & Unwrapping">

/**
 * Implement this on your errors  to control which exception is raised on calling
 * [Failure.get] function.
 *
 * @param X
 *      The type throwable this failure wants use when caller tries to get a success value
 *      from this error
 */
fun interface ThrowableProvider<out X : Throwable> {
    fun throwing(): X

    companion object {
        fun <X : Throwable> of(cause: X) = ThrowableProvider { cause }
    }
}


/**
 * Implement this if interface on any exception provided by the [ThrowableProvider.failure] call
 * if you want your own exception to be able to unwrap a [Failure].
 *
 * By default, this library (if not provided with one), will use an internal implementation.
 *
 * @see DefaultFailureUnwrappingException
 *      The internal exception used by the library to unwrap a failure from.
 * @see Throwable.unwrapOrNull
 *      A function which attempts to unwrap a specific `Failure` based on desired error value type.
 * @see resultOf
 */
interface FailureUnwrappingCapable<out E> {
    fun unwrapFailure(): Failure<out E>?
}

/**
 * The purpose of this class is to bridge the functional model of the [Result] operations with the traditional
 * _try-catch_ world of Object-Oriented contract which specifies that failures should be raised and the current
 * operation should be aborted.
 *
 * @constructor Creates a new wrapped failure exception which can be raised using the `throw` operation.
 * @param wrapped The failure to wrap.
 * @see Result.get
 * @see resultWithHandlingOf
 */
@Suppress("UNCHECKED_CAST")
class DefaultFailureUnwrappingException(
    wrapped: Failure<*>,
) : RuntimeException("${wrapped.error}"), FailureUnwrappingCapable<Any> {
    private val _wrapped = wrapped as Failure<Any>
    override fun unwrapFailure(): Failure<out Any> = _wrapped
}

@Suppress("UNCHECKED_CAST")
inline fun <reified E> Throwable.unwrapOrNull(): Failure<E>? {
    return when (this) {
        is E -> Failure(this)
        !is FailureUnwrappingCapable<*> -> null
        else -> unwrapFailure()?.takeIf { it.error is E }?.let { it as Failure<E> }
    }
}

inline fun <reified E> Throwable.unwrapErrorOrNull(): E? {

    if (this is E) {
        return this
    }

    val failure = (this as? FailureUnwrappingCapable<*>)?.unwrapFailure() ?: return null
    val error = failure.error ?: return null
    return error as? E

}

//</editor-fold>

