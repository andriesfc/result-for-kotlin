package resultk

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isSuccess
import assertk.assertions.messageContains
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.interop.toStdLibResult
import resultk.demo.domain.ErrorCaseEnum

@DisplayName("InterOp with Kotlin's own builtin Result type")
class InteropStdResultTest {

    @Test
    fun `Failure result should return a standard library result which reports an exception`() {
        val expectedError = ErrorCaseEnum.ERROR_CASE_1
        val stdLibResult = result<ErrorCaseEnum,Int> { expectedError.failure() }.toStdLibResult().also(::println)
        assertThat { stdLibResult.getOrThrow() }.isFailure().messageContains(expectedError.toString())
    }

    @Test
    fun `Success result should return standard library result with correct value and does not throw any exception`() {
        val successValue = 12
        val stdLibResult = result<ErrorCaseEnum,Int> { successValue.success()  }.toStdLibResult().also(::println)
        assertThat { stdLibResult.getOrThrow() }.isSuccess().isEqualTo(successValue)
    }
}