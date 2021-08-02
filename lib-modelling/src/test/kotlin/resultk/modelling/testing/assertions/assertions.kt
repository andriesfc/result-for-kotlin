package resultk.modelling.testing.assertions

import assertk.Assert
import assertk.all
import assertk.assertions.isNotEmpty
import assertk.assertions.prop

fun Assert<String>.isNotEmptyOrBlank() = apply {
    all("isNotEmptyOrBlank") {
        isNotEmpty()
        prop("trimmed", String::trim).isNotEmpty()
    }
}

fun <T> Assert<T>.peek(action: (T) -> Unit) = apply { given(action) }