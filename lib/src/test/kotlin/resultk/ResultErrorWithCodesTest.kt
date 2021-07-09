package resultk

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import resultk.ResultErrorWithCodesTest.WasherFailure.WasherMotorControlFailure
import resultk.assertions.error


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResultErrorWithCodesTest {

    @Test
    fun usingSealedClassesAsErrorCodes() {

        val controllerAddressExpected = 7781L
        val controllerStateExpected = "S001"

        val failure = WasherMotorControlFailure(controllerAddressExpected, controllerStateExpected)
            .failure<WasherMotorControlFailure, Any>()

        assertThat(failure).error().all {
            transform("cause") { it.cause }.isNull()
            transform("failureState") { it.failureState }.isEqualTo(controllerStateExpected)
            transform("controllerAddress") { it.controllerAddress }.isEqualTo(controllerAddressExpected)
            transform("throwable()") { it.throwable() }
                .isNotNull()
                .isInstanceOf(RuntimeException::class)
                .cause().isNull()
        }
    }


    @ParameterizedTest
    @EnumSource
    fun test_enums_as_error(ce: ControllerErrorCode) {
        val r = ce.failure<ControllerErrorCode, Any>()
        assertThat(r).error().all {
            transform("description") { it.description }.isEqualTo(ce.description)
            transform("throwable()") { it.throwable() }.all {
                isInstanceOf(ControllerErrorCode.UnhandledControllerException::class)
                transform { it.cause }.isNull()
            }
            assertThat { r.get() }.isFailure().isInstanceOf(ControllerErrorCode.UnhandledControllerException::class)
        }
    }


    sealed class WasherFailure<T>(
        val code: T,
        val cause: Exception? = null
    ) :
        Result.Failure.Throwable {
        abstract val description: String

        class WasherMotorControlFailure(
            val controllerAddress: Long,
            val failureState: String
        ) : WasherFailure<String>(code = "[AB1:${controllerAddress.toString(16)}:$failureState]") {
            override val description: String = "Motor control failure, for details report"
        }

        override fun throwable(): Throwable {
            return RuntimeException("[$code] $description", cause)
        }
    }

    enum class ControllerErrorCode(val description: String) : Result.Failure.Throwable {

        CN_001("Unexpected down"),
        CN_002("Unexpected up.")
        ;

        inner class UnhandledControllerException : RuntimeException(
            "Unhandled controller error: [${name}] $description"
        )

        override fun throwable(): Throwable = UnhandledControllerException()
    }

}