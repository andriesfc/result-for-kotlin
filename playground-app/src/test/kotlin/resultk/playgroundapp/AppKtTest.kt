package resultk.playgroundapp

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.util.*

internal class AppKtTest {

    @Suppress("UNCHECKED_CAST")
    private val props = StringReader(
        """
        project.description=Domain driven result and error handling for Modern Kotlin
        project.inceptionYear=2021
        project.developer=
        project.developer.andriesfc.email=andriesfc@gmail.com
        project.developer.andriesfc.timezone=GMT+2:00
        project.developer.andriesfc.role.1=Lead Developer
        project.developer.andriesfc.org.name=AndriesFC
        project.developer.andriesfc.org.url=https://github.com/andriesfc
        project.developer.jane
        """.trimIndent()
    ).use { Properties().apply { load(it) } }.toMap() as Map<String, String?>

    @Test
    fun testRecord() {
        assertThat { props.records("project.developer") }
            .isSuccess()
            .peek { println("records = %s".format(it)) }
            .isEqualTo(
                mapOf(
                    "jane" to mapOf(),
                    "andriesfc" to mapOf(
                        "email" to "andriesfc@gmail.com",
                        "timezone" to "GMT+2:00",
                        "role.1" to "Lead Developer"
                    )
                )
            )
    }

    @Test
    fun testTrimMargins() {
        val given = "\"Core library supporting domain Driven result &amp; error handling library for modern Kotlin\""
        val expected = "Core library supporting domain Driven result &amp; error handling library for modern Kotlin"
        assertThat(given.trimMargins({it == '"'}, {it == '"'})).isEqualTo(expected)
    }
}


private fun <T> Assert<T>.peek(a: (T) -> Unit): Assert<T> = apply { given(a) }