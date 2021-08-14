package resultk

import assertk.assertThat
import assertk.assertions.*
import io.mockk.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.assertions.isSuccessResult
import resultk.demo.domain.ErrorCaseEnum
import java.io.IOException
import java.time.LocalDate

@DisplayName("Accessing success values and failures test")
internal class AccessingValuesTest {

    @Test
    fun `onSuccess should be able access success value if available`() {
        val expectedValue = 12
        val mockOnSuccess = mockk<(Int) -> Unit>()
        val successSlot = slot<Int>()
        every { mockOnSuccess(capture(successSlot)) } just Runs
        val success = expectedValue.success<String, Int>()
        success.onSuccess(mockOnSuccess)
        verify { mockOnSuccess(any()) }
        assertThat(successSlot.isCaptured).isTrue()
        assertThat(successSlot.captured).isEqualTo(expectedValue)
    }

    @Test
    fun `onFailure should be able access success error if available`() {
        val expectedError = "error_12"
        val errorSlot = slot<String>()
        val mockOnFailure = mockk<(String) -> Unit>()
        every { mockOnFailure(capture(errorSlot)) } just Runs
        val failure = expectedError.failure<String, Int>()
        failure.onFailure(mockOnFailure)
        verify { mockOnFailure(any()) }
        assertThat(errorSlot.isCaptured).isTrue()
        assertThat(errorSlot.captured).isEqualTo(expectedError)
    }

    @Test
    fun `errorOrNull on result should produce a null if te result is a success`() {
        val result = 12.success<Exception, Int>()
        assertThat(result.errorOrNull()).isNull()
    }

    @Test
    fun `errorOrNull on result should produce an error value is te result is a failure`() {
        val expectedError = IOException()
        val result = expectedError.failure<Exception, Int>()
        assertThat(result.errorOrNull()).isNotNull().isEqualTo(expectedError)
    }

    @Test
    fun `Destructuring a success result should produce a null (error) value in the 2nd part`() {
        val expectedSuccess = 12
        val (r, e) = expectedSuccess.success<IOException, Int>()
        assertThat(e).isNull()
        assertThat(r).isSuccessResult().isEqualTo(expectedSuccess)
    }

    @Test
    fun `Caller should be able to handle possible failure when retrieving the success value with a lambda`() {
        val today = LocalDate.now()
        val incidentReportedDate = ErrorCaseEnum.ERROR_CASE_1.failure<ErrorCaseEnum, LocalDate>()
        val indentReportErrorIsToday = fun(_: ErrorCaseEnum) = today
        val reportActionDue = incidentReportedDate.or(indentReportErrorIsToday)
        assertThat(reportActionDue).isEqualTo(today)
    }

    @Test
    fun `Caller should be able to handle possible failure when retrieving the success by supplying a value`() {
        val today = LocalDate.now()
        val incidentReportedDate = ErrorCaseEnum.ERROR_CASE_1.failure<ErrorCaseEnum, LocalDate>()
        val reportActionDue = incidentReportedDate.or(today)
        assertThat(reportActionDue).isEqualTo(today)
    }

    @Test
    fun `Caller should be able to throw a specific exception if the result is a failure`() {

        class ReportingAbortedException : Exception()

        val incidentDueDate = ErrorCaseEnum.ERROR_CASE_1.failure<ErrorCaseEnum, LocalDate>()

        assertThat { incidentDueDate.orThrow { ReportingAbortedException() } }
            .isFailure()
            .isInstanceOf(ReportingAbortedException::class)
    }

}