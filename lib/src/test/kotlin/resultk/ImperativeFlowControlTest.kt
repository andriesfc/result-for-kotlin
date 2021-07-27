package resultk

import assertk.assertThat
import assertk.assertions.*
import resultk.Result.Failure
import resultk.Result.Success
import resultk.demo.domain.ErrorCaseEnum
import kotlin.test.Test

/**
 * Test classes exercising imperative control flow to establish a single happy path.
 */
internal class ImperativeFlowControlTest {

    @Test
    fun `Any error code could also be treated as an exceptional flow`() {
        val anyErrorCode = "thisAsAnyErrorCode"
        assertThat { raise(anyErrorCode) }
            .isFailure()
            .isInstanceOf(Throwable::class)
            .transform("unwrappedErrorCodeOfNull") { it.unwrapErrorOrNull<String>() }
            .isNotNull()
            .isEqualTo(anyErrorCode)
    }

    @Test
    fun `Demonstrate imperative style of a fold operation`() {

        val testCases = listOf(
            Failure(ErrorCaseEnum.ERROR_CASE_1) to ErrorCaseEnum.ERROR_CASE_1.toString(),
            Success(1) to "1"
        )

        fun imperativeFlowToString(r: Result<ErrorCaseEnum, Int>): String {
            val (n, err) = r
            return err?.toString() ?: n.get().toString()
        }

        assertThat(testCases).each { assert ->
            assert.given { (r, expected) ->
                assertThat(imperativeFlowToString(r)).isEqualTo(expected)
            }
        }
    }

}