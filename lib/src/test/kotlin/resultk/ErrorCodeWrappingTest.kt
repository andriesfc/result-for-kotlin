package resultk

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.domain.SimpleErrorEnum
import resultk.testing.domain.ErrorEnumWSelfUnwrapping

@DisplayName("Error code wrapping tests")
internal class ErrorCodeWrappingTest {

    @Test
    fun unwrapping_from_builtin_provider_should_return_error() {
        val expected = SimpleErrorEnum.ERROR_CASE_1
        val result: Result<SimpleErrorEnum, Int> = expected.failure()
        assertThat { result.get() }
            .isFailure()
            .isInstanceOf(Result.Failure.FailureUnwrapper::class)
            .transform { it.unwrap()?.error }
            .isEqualTo(expected)
    }

    @Test
    fun unwrapping_from_custom_provider_should_return_error() {
        val expected = ErrorEnumWSelfUnwrapping.ERROR_CASE_1
        val result: Result<ErrorEnumWSelfUnwrapping, Int> = expected.failure()
        assertThat { result.get() }
            .isFailure()
            .isInstanceOf(ErrorEnumWSelfUnwrapping.CustomFailureUnwrapper::class)
            .transform { it.unwrap()?.error }
            .isEqualTo(expected)
    }

}