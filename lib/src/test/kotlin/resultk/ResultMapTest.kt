package resultk

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.assertions.isFailureResult
import resultk.demo.domain.ErrorCaseEnum
import resultk.testing.assertions.isSuccessResult

@DisplayName("Mapping success values and failures test")
internal class ResultMapTest {

    @Test
    fun `Mapping a success value from an failure result should return the exact same instance`() {
        val result: Result<ErrorCaseEnum, Int> = result { ErrorCaseEnum.ERROR_CASE_1.failure() }
        val mapped = result.map(Int::toString)
        assertThat(mapped).isSameAs(result)
    }

    @Test
    fun `Mapping a failure value from an success result should return the exact same instance`() {
        val result: Result<ErrorCaseEnum, Int> = result { 10.success() }
        val mapped = result.mapFailure(ErrorCaseEnum::name)
        assertThat(mapped).isSameAs(result)
    }

    @Test
    fun `Mapping a success value`() {
        val successValue = 13
        val successResult = result<ErrorCaseEnum, Int> { successValue.success() }
        val mapped = successResult.map { it.toString(2) }
        assertThat(mapped).isSuccessResult().isEqualTo("1101")
    }

    @Test
    fun `Mapping a failure value`() {
        val failureValue = "ERROR_CASE_1"
        val expectedValue = ErrorCaseEnum.ERROR_CASE_1
        val failureResult = failureValue.failure<String, Int>()
        val mapped = failureResult.mapFailure { ErrorCaseEnum.valueOf(it) }
        assertThat(mapped).isFailureResult().isEqualTo(expectedValue)
    }

    @Test
    fun `Fold operation on a failure result should not invoke the 'onSuccess'`() {
        val value = ErrorCaseEnum.ERROR_CASE_2
        val failureResult = result<ErrorCaseEnum, Int> { value.failure() }
        val onSuccess = spyk<(Int) -> String>({ it.toString() })
        val onFailure = spyk<(ErrorCaseEnum) -> String>({ it.name })
        val s = failureResult.fold(onFailure, onSuccess)
        assertThat(s).isEqualTo(value.toString())
        verify(exactly = 0) { onSuccess(any()) }
        verify(exactly = 1) { onFailure(any()) }
    }

    @Test
    fun `Fold operation on a success result should not invoke the 'onFailure'`() {
        val value = 12
        val result = result<ErrorCaseEnum, Int> { value.success() }
        val onSuccess = spyk<(Int) -> String>({ it.toString() })
        val onFailure = spyk<(ErrorCaseEnum) -> String>({ it.name })
        val s = result.fold(onFailure, onSuccess)
        assertThat(s).isEqualTo(value.toString())
        verify(exactly = 1) { onSuccess(any()) }
        verify(exactly = 0) { onFailure(any()) }
    }
}