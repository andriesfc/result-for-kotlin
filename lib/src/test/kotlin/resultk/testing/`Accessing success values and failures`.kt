package resultk.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.mockk.*
import org.junit.jupiter.api.Test
import resultk.*
import java.io.IOException

internal class `Accessing success values and failures` {

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
        assertThat(r.isSuccess).isTrue()
        assertThat(r.get()).isEqualTo(expectedSuccess)
    }
}