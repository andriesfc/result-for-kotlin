package io.github.andriesfc.kotlin.result

import io.github.andriesfc.kotlin.result.Result.Failure
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class WrappedFailureAsExceptionTest {

    @Test
    fun wrapping_non_throwable_failure() {
        assertDoesNotThrow { WrappedFailureAsException(Failure(1))  }
    }

    @Test
    fun unwrapping_any_exception() {
        val expectedError = 1
        val e = assertThrows<Throwable> { Failure(expectedError).get()  }
        assertTrue(e.unwrapFailure().isPresent)
        assertEquals(expectedError, e.unwrapFailure().get().error)
    }

}