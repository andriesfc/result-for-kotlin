package resultk.interop

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import resultk.testing.assertions.isFailure
import resultk.testing.assertions.isSuccess
import resultk.result
import resultk.success

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Tests basic handling of failures")
internal class BasicFailureHandlingTest {

    @Test
    fun convert_to_standard_lib_result_when_there_is_an_failure() {
        val r = runCatching { 1 / 0 }.toResult()
        assertThat(r).isFailure()
            .isInstanceOf(ArithmeticException::class)
            .messageContains("/ by zero")
    }

    @Test
    fun convert_to_standard_lib_result_when_there_is_no_failure() {
        val r = runCatching { 1 + 10 }.toResult()
        assertThat(r).isSuccess().isEqualTo(1 + 10)
    }

    @Test
    fun convert_lib_standard_to_result_when_there_is_no_failure() {
        val r = result<Exception, Int> { (1 + 10).success() }.toStandard()
        val expected = runCatching { 1 + 10 }
        assertThat(r).isEqualTo(expected)
    }
}