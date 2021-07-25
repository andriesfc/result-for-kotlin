package resultk

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import org.apache.commons.codec.binary.Hex.encodeHexString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.assertions.isFailureResult
import resultk.testing.assertions.isSuccessResult
import resultk.demo.domain.ErrorCaseEnum
import java.io.IOException
import java.security.MessageDigest
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

@DisplayName("Functional flow and controlled processing test")
internal class FunctionFlowControlTest {

    @Test
    fun `Use 'resultCatching()' with caught handler convert expected 'Throwable' to expected error`() {
        val expectedErrorCase = ErrorCaseEnum.ERROR_CASE_2
        val exceptionAsErrorCase2 = fun(_: IOException) = expectedErrorCase
        assertThat {
            resultWithHandlingOf<IOException, ErrorCaseEnum, Int>(exceptionAsErrorCase2) {
                throw IOException()
            }
        }.isSuccess().isFailureResult().isEqualTo(expectedErrorCase)
    }

    @Test
    fun `Using 'thenResultCatching()' to invoke the next call in the call chain`() {

        val localDate = LocalDate.of(2021, Month.JANUARY, 16)
        val localIsoDateString = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val callChain = {

            val log = fun(ex: Exception) = ex.printStackTrace(System.err)
            val parsingFailureAsErrorCase1 = { ex: Exception -> ErrorCaseEnum.ERROR_CASE_1.also { log(ex) } }
            val dateCreationFailureAsErrorCase2 = { ex: Exception -> ErrorCaseEnum.ERROR_CASE_2.also { log(ex) } }
            fun String.toNumberStr(): String = trimEnd().trimStart { char -> char == '0' || !char.isDigit() }

            resultWithHandlingOf(parsingFailureAsErrorCase1) {
                println("Local ISO Date = $localDate")
                localIsoDateString
                    .splitToSequence('-')
                    .map(String::toNumberStr)
                    .map(String::toInt)
                    .take(3)
                    .toList().also(::println)
                    .success()
            }.thenResultOfHandling(dateCreationFailureAsErrorCase2) {
                val (year, month, day) = result
                LocalDate.of(year, month, day).success()
            }
        }

        assertThat(callChain::invoke)
            .isSuccess()
            .isSuccessResult().isEqualTo(localDate)
    }

    @Test
    fun `Use 'thenResult()' result to construct a single happy path`() {

        fun String.computedSha1(): Result<Exception, String> {
            return resultOf<Exception, MessageDigest> {
                MessageDigest.getInstance("sha1").success()
            }.thenResultOf {
                result.update(this@computedSha1.toByteArray(Charsets.US_ASCII))
                encodeHexString(result.digest()).success()
            }
        }

        val input = "JellyBeansInTheHouse"
        val expected = "08e567e0cfd91547358beaff374d3b35b4131c34"

        println(input.computedSha1())

        assertThat(input::computedSha1)
            .isSuccess()
            .isSuccessResult()
            .isEqualTo(expected)

    }

}