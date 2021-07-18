package resultk

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.assertions.isSuccessResult

@DisplayName("Core Data types and special values tests")
internal class CoreDataStructureTest {

    @Test
    fun `Is is possible to return unit as result`() {
        val unitAsResult: Result<Boolean, Unit> = Result.Success(Unit)
        assertThat(unitAsResult).isSuccessResult().isEqualTo(Unit)
    }

    @Test
    fun `Calling get on failure always result on an exception`() {
        val failure = Result.Failure(1)
        assertThat(failure::get).isFailure().transform {  }
    }

    @Test
    fun `Calling get on un throwable error always wraps`() {
        val error = 1
        val failure = Result.Failure(error)
        assertThat(failure::get).isFailure()
            .isInstanceOf(Result.Failure.FailureUnwrappingCapable::class)
            .transform("wrapped().error") { provider -> provider.unwrap()?.error }
            .isEqualTo(error)

    }

    @Test
    fun `Failure result should identify correctly`() {
        val failure: Result<Int, String> = Result.Failure(10)
        assertThat(failure, "failure").all {
            prop(Result<Int, String>::isFailure).isEqualTo(true)
            prop(Result<Int, String>::isSuccess).isEqualTo(false)
        }
    }

    @Test
    fun `Success result should identity correctly`() {
        val value = "ok"
        val success: Result<Int, String> = Result.Success(value)
        assertThat(success, "success").all {
            prop(Result<Int, String>::isFailure).isEqualTo(false)
            prop(Result<Int, String>::isSuccess).isEqualTo(true)
        }
    }

    @Test
    fun `Unit as success value should always return constant value`() {

        fun sayHello() {
            println("Hello from test!")
        }

        val result: Result<Exception, Unit> = resultOf { Result.Success(sayHello()) }

        assertThat(result).isSuccessResult().isSameAs(Unit)
    }
}