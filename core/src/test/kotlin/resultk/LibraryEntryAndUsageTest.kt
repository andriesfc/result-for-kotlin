package resultk

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import assertk.assertions.messageContains
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.demo.domain.ErrorEnumWSelfUnwrapping
import resultk.testing.assertions.isFailureResult
import resultk.testing.assertions.isSuccessResult
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter.BASIC_ISO_DATE
import java.util.*
import kotlin.random.Random

@DisplayName("Creating results & result entry points tests")
internal class LibraryEntryAndUsageTest {

    @Test
    fun `Returning success value from receiver`() {
        val successValue = "ok"
        val success = successValue.success<Any, String>()
        assertThat(success).isSuccessResult().isEqualTo(successValue)
    }

    @Test
    fun `Returning error value from receiver`() {
        val errorTraceCode = "E_%s".format(UUID.randomUUID())
        val result = errorTraceCode.failure<String, Long>()
        assertThat(result).isFailureResult().isEqualTo(errorTraceCode)
    }

    @Test
    fun `Result action throwing exception which can unwrap should unwrap and not throw`() {
        assertThat {
            resultOf<ErrorEnumWSelfUnwrapping, Int> {
                throw ErrorEnumWSelfUnwrapping.ERROR_CASE_1.throwing()
            }
        }.isSuccess().isFailureResult().isEqualTo(ErrorEnumWSelfUnwrapping.ERROR_CASE_1)
    }

    @Test
    fun `Immediate produce a results from 'this' value`() {

        val logfileDate = LocalDate.now().run {
            val lastMonth = month - 1
            val date = Random.nextInt(1, lastMonth.maxLength())
            LocalDate.of(year, lastMonth, date)
        }.format(BASIC_ISO_DATE)

        val logFile = File("/var/logs/dora/298/failures.$logfileDate.log")
        val logFileSize = logFile.resultWithHandling {
            if (!exists()) throw FileNotFoundException(path)
            length().success<IOException,Long>()
        }

        assertThat(logFileSize).isFailureResult()
            .isInstanceOf(FileNotFoundException::class)
            .messageContains(logFile.path)
    }
}