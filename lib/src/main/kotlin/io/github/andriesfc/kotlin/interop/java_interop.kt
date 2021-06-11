@file:JvmName("JavaInterop")
package io.github.andriesfc.kotlin.interop

import java.util.function.Consumer


/**
 * Produces a function from this Java [Consumer] to a function which takes an instance
 * of [T] and passed to the back to this [Consumer.accept] function.
 */
fun <T> Consumer<T>.accepting(): ((T) -> Unit) = { accept(it) }