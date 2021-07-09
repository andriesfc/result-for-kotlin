package resultk.interop

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import resultk.assertions.error
import resultk.assertions.value
import resultk.result
import resultk.success

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StdlibKtTest {

    @Test
    fun convertToStandardLibResultKWhenThereIsAnFailure() {
        val r = runCatching { 1 / 0 }.toResult()
        assertThat(r).error()
            .isInstanceOf(ArithmeticException::class)
            .messageContains("/ by zero")
    }

    @Test
    fun convertToStandardLibResultLWhenThereIsNoFailure() {
        val r = runCatching { 1 + 10 }.toResult()
        assertThat(r).value().isEqualTo(1 + 10)
    }

    @Test
    fun convertLibStandardToResultKWhenThereIsNoFailure() {
        val r = result<Exception, Int> { (1 + 10).success() }.toCompatible()
        val expected = runCatching { 1 + 10 }
        assertThat(r).isEqualTo(expected)
    }
}