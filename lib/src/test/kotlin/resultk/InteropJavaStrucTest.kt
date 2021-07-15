package resultk

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.matchesPredicate
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.interop.java.toOptional
import resultk.interop.java.toResult
import resultk.testing.assertions.isFailureResult
import resultk.testing.assertions.isSuccessResult
import resultk.testing.domain.ErrorCaseEnum
import java.util.*

@DisplayName("Testing interop with Java's standard Optional type")
internal class InteropJavaStrucTest {

    @Test
    fun `An empty Java 'Optional() has not notion of an error type and should throw an exception'`() {
        val result = Optional.empty<String>().toResult().also(::println)
        assertThat(result::get).isFailure().isInstanceOf(NoSuchElementException::class)
    }

    @Test
    fun `An empty java 'Optional' should be fully converted to a result if a missing error value is supplied`() {
        val result = Optional.empty<Int>().toResult { ErrorCaseEnum.ERROR_CASE_1 }.also(::println)
        assertThat(result).isFailureResult().isEqualTo(ErrorCaseEnum.ERROR_CASE_1)
    }

    @Test
    fun `A non empty 'Optional' should be able to converted to a valid result`() {
        val nonEmptyOptional = Optional.of(6)
        val result = nonEmptyOptional.toResult().also(::println)
        assertThat(result).isSuccessResult().isEqualTo(nonEmptyOptional.get())
    }

    @Test
    fun `A success result should represent a non-empty optional`() {
        val successValue = 16
        val successOption = successValue.success<ErrorCaseEnum, Int>().toOptional()
        assertThat(successOption).all {
            matchesPredicate { it.isPresent }
            transform { it.get() }.isEqualTo(successValue)
        }
    }

    @Test
    fun `A failure result should represent an empty optional`() {
        val failureValue = result<Exception, Int> { throw IllegalStateException() }
        val optional = failureValue.toOptional()
        assertThat(optional).matchesPredicate { it.isEmpty }
    }
}