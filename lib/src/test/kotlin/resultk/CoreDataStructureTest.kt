package resultk

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.assertions.isSuccess

@DisplayName("Core Data types and special values tests")
internal class CoreDataStructureTest {

    @Test
    fun is_is_possible_to_return_unit_as_result() {
        val unitAsResult: Result<Boolean, Unit> = Result.Success(Unit)
        assertThat(unitAsResult).isSuccess().isEqualTo(Unit)
    }

    @Test
    fun calling_get_on_failure_always_result_on_an_exception() {
        val failure = Result.Failure(1)
        assertThat(failure::get).isFailure()
    }

    @Test
    fun calling_get_on_un_throwable_error_always_wraps() {
        val error = 1
        val failure = Result.Failure(error)
        assertThat(failure::get).isFailure()
            .isInstanceOf(Result.Failure.FailureUnwrapper::class)
            .transform("wrapped().error") { provider -> provider.unwrap()?.error }
            .isEqualTo(error)

    }

    @Test
    fun failure_result_should_identify_correctly() {
        val failure: Result<Int, String> = Result.Failure(10)
        assertThat(failure, "failure").all {
            prop("isFailure", Result<Int, String>::isFailure).isEqualTo(true)
            prop("isSuccess", Result<Int, String>::isSuccess).isEqualTo(false)
        }
    }

    @Test
    fun success_result_should_identity_correctly() {
        val value = "ok"
        val success: Result<Int, String> = Result.Success(value)
        assertThat(success, "success").all {
            prop("isFailure", Result<Int, String>::isFailure).isEqualTo(false)
            prop("isSuccess", Result<Int, String>::isSuccess).isEqualTo(true)
        }
    }

    @Test
    fun unit_as_success_value_should_always_return_constant_value() {

        fun sayHello() {
            println("Hello from test!")
        }

        val result: Result<Exception, Unit> = result { Result.Success(sayHello()) }

        assertThat(result).isSuccess().isEqualTo(Unit)
    }
}