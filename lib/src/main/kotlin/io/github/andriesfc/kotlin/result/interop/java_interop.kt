@file:JvmName("JavaInterop")
package io.github.andriesfc.kotlin.result.interop

import io.github.andriesfc.kotlin.result.onFailure
import io.github.andriesfc.kotlin.result.onSuccess
import java.util.function.Consumer


/**
 * This converts any Java [Consumer] instance to a kotlin lambda in the form of
 * `(T)->Unit`.
 *
 * @see onSuccess
 * @see onFailure
 */
fun <T> Consumer<T>.accepting(): ((T) -> Unit) = { accept(it) }