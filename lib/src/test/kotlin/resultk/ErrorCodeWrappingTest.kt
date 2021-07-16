package resultk

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.domain.ErrorCaseEnum
import resultk.testing.domain.ErrorEnumWSelfUnwrapping

@DisplayName("Error code wrapping tests")
internal class ErrorCodeWrappingTest {

    @Test
    fun `Unwrapping from builtin provider should return error`() {
        val expected = ErrorCaseEnum.ERROR_CASE_1
        val result: Result<ErrorCaseEnum, Int> = expected.failure()
        assertThat { result.get() }
            .isFailure()
            .isInstanceOf(Result.Failure.FailureUnwrapper::class)
            .transform { it.unwrap()?.error }
            .isEqualTo(expected)
    }

    @Test
    fun `Unwrapping from custom provider should return error`() {
        val expected = ErrorEnumWSelfUnwrapping.ERROR_CASE_1
        val result: Result<ErrorEnumWSelfUnwrapping, Int> = expected.failure()
        assertThat { result.get() }
            .isFailure()
            .isInstanceOf(ErrorEnumWSelfUnwrapping.CustomFailureUnwrapper::class)
            .transform { it.unwrap()?.error }
            .isEqualTo(expected)
    }

}