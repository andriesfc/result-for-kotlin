
@file:JvmName("ResultFailureWrapping")
package io.github.andriesfc.resultk

import io.github.andriesfc.resultk.Result.Failure

/**
 * Indicates that an error occurred, but the [Failure.error] itself could not be thrown as it
 * is not a `Throwable` type.
 *
 * @property wrappedFailure Reports the actual wrapped failure.
 *
 * @see UnsafeGet.get
 * @see Result.Failure
 * @see resultOf
 */
class WrappedUnThrowableFailureException private constructor(
    message: String,
    val wrappedFailure: Result.Failure<*>
) : RuntimeException(message) {

    internal companion object {
        /**
         * Raises a specific failure eiter as [WrappedUnThrowableFailureException] - if the [Failure.error] is not
         * a [Throwable], or throw the actual [Failure.error] produced.
         */
        @JvmStatic
        internal fun raise(failure: Result.Failure<*>): Nothing = throw when (failure.error) {
            is Throwable -> failure.error
            else -> WrappedUnThrowableFailureException("Operation failed with: ${failure.error}", failure)
        }
    }

    /**
     * Unwraps a [wrappedFailure] as a specific [Failure.error] type [ofClass]
     *
     *
     * @param ofClass The expected type the callers wants to unwrap.
     * @param E The generic type placeholder.
     *
     * @return The unwrapped failure, or `null` if the failure error is not of the correct type.
     */
    fun <E> unwrapAs(ofClass: Class<E>):Failure<E>? {
        @Suppress("UNCHECKED_CAST")
        return when  {
            ofClass.isInstance(wrappedFailure.error) -> wrappedFailure as Failure<E>
            else -> null
        }
    }

    /**
     * Reified version of `unwrapAs` function.
     */
    inline fun <reified E> unwrapAs():Failure<E>? {
        return  unwrapAs(E::class.java)
    }
}

/**
 * Try to unwrap an [Failure] [ofClass] from a specific throwable.
 *
 * @param ofClass The expected [Failure.error]  type.
 *
 * @return The [Failure], or `null` if the receiver is not a [WrappedUnThrowableFailureException] or the
 * [WrappedUnThrowableFailureException.wrappedFailure] does not hold a error [ofClass].
 *
 * @see WrappedUnThrowableFailureException.unwrapAs
 */
fun <E> Throwable.unwrapAs(ofClass:Class<E>): Failure<E>? {
    return when (this) {
        is WrappedUnThrowableFailureException -> this.unwrapAs(ofClass)
        else -> null
    }
}

/**
 * Reified version of the `unwrapAs` function.
 */
inline fun <reified E> Throwable.unwrapAs():Failure<E>? {
    return when (this) {
        is WrappedUnThrowableFailureException -> this.unwrapAs()
        else -> null
    }
}