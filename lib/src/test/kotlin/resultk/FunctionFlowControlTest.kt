package resultk

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.assertions.isFailure
import resultk.testing.domain.ErrorCaseEnum
import java.io.IOException

@DisplayName("Functional flow and controlled processing test")
internal class FunctionFlowControlTest {

    @Test
    fun `resultCatching with caught handler should convert expected_exception`() {
        val expectedErrorCase = ErrorCaseEnum.ERROR_CASE_2
        val ioExceptionAsErrorCase2 = { _: IOException -> expectedErrorCase }
        assertThat {
            val r: Result<ErrorCaseEnum, Int> = resultCatching(ioExceptionAsErrorCase2) {
                throw IOException()
            }
            r
        }.isSuccess().isFailure().isEqualTo(expectedErrorCase)
    }

}