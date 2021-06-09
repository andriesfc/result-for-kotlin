
@file:JvmName("UnhandledFailureExceptionOperations")
package io.github.andriesfc.resultk

import io.github.andriesfc.resultk.Result.Failure

/**
 * Indicates that an error occurred, but the [Failure.error] itself could not be thrown as it
 * is not a `Throwable` type.
 *
 * @property failure Reports the actual wrapped failure.
 *
 * @aee Result.get
 * @see Result.Failure
 * @see resultOf
 */
class UnhandledFailureException internal constructor(
    val failure: Failure<*>
) : RuntimeException("Unhandled error raised: ${failure.error}")


inline val Throwable.failureOrNull: Failure<Any>?
    get() = when (this) {
        is UnhandledFailureException -> failure.castAsOrNull()
        else -> null
    }