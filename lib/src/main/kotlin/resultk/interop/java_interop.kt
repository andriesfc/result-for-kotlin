@file:JvmName("JavaInterop")
package resultk.interop

import resultk.onFailure
import resultk.onSuccess
import java.util.function.Consumer


/**
 * This converts any Java [Consumer] instance to a kotlin lambda in the form of
 * `(T)->Unit`.
 *
 * @see onSuccess
 * @see onFailure
 */
fun <T> Consumer<T>.accepting(): ((T) -> Unit) = { accept(it) }
