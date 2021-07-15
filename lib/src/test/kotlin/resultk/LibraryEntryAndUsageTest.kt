package resultk

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.assertions.*
import resultk.testing.domain.ErrorEnumWSelfUnwrapping
import resultk.testing.domain.ErrorCaseEnum
import java.io.IOException
import java.util.*

@DisplayName("Creating results & result entry points tests")
internal class LibraryEntryAndUsageTest {

    @Test
    fun returning_success_value_from_receiver() {
        val successValue = "ok"
        val success = successValue.success<Any, String>()
        assertThat(success)
            .isInstanceOf(Result.Success::class)
            .transform("value") { it.value }
            .isEqualTo(successValue)
    }

    @Test
    fun returning_error_value_from_receiver() {
        val errorTraceCode = "E_%s".format(UUID.randomUUID())
        val result = errorTraceCode.failure<String, Long>()
        assertThat(result)
            .isInstanceOf(Result.Failure::class)
            .transform("error", Result.Failure<*>::error)
            .isEqualTo(errorTraceCode)
    }

    @Test
    fun result_action_throwing_exception_which_can_unwrap_should_unwrap_and_not_throw() {
        assertThat {
            result<ErrorEnumWSelfUnwrapping, Int> {
                throw ErrorEnumWSelfUnwrapping.ERROR_CASE_1.throwable()
            }
        }.isSuccess()
    }
}