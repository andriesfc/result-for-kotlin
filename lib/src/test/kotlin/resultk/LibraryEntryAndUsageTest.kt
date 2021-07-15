package resultk

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.assertions.isFailureResult
import resultk.testing.assertions.isSuccessResult
import resultk.testing.domain.ErrorEnumWSelfUnwrapping
import java.util.*

@DisplayName("Creating results & result entry points tests")
internal class LibraryEntryAndUsageTest {

    @Test
    fun `Returning success value from receiver`() {
        val successValue = "ok"
        val success = successValue.success<Any, String>()
        assertThat(success).isSuccessResult().isEqualTo(successValue)
    }

    @Test
    fun `Returning error value from receiver`() {
        val errorTraceCode = "E_%s".format(UUID.randomUUID())
        val result = errorTraceCode.failure<String, Long>()
        assertThat(result).isFailureResult().isEqualTo(errorTraceCode)
    }

    @Test
    fun `Result action throwing exception which can unwrap should unwrap and not throw`() {
        assertThat {
            result<ErrorEnumWSelfUnwrapping, Int> {
                throw ErrorEnumWSelfUnwrapping.ERROR_CASE_1.throwable()
            }
        }.isSuccess().isFailureResult().isEqualTo(ErrorEnumWSelfUnwrapping.ERROR_CASE_1)
    }
}