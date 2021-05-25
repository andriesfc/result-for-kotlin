package andriesfc.resultk

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.util.UUID.randomUUID
import kotlin.test.assertTrue


@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResultDemo {

    private interface TextReader {
        fun readText(): Result<IOException, String>
    }

    @Nested
    inner class ProceduralStyleHandling {

        @Test
        fun handle_error_explicit() {
            whenReadTextReportIOExceptionToCaller()
            val (get, e) = textReader.readText()
            assertThat(e).isNotNull()
            assertTrue { e is IOException }
            assertThrows<IOException> { get() }
        }

        @Test
        fun handle_error_with_try_catch() {
            whenReadTextReportIOExceptionToCaller()
            var caught: IOException? = null
            try {
                val (get) = textReader.readText()
                get()
            } catch (e: IOException) {
                caught = e
            }

            assertThat(caught).isNotNull()
        }

        @Test
        fun get_result_of_text_reader_if_exists() {
            val expectedText = "expected text ${randomUUID()}"
            whenReadTextReturnWith(expectedText)
            assertThat { textReader.readText().get() }.isSuccess().isEqualTo(expectedText)
        }

    }

    private fun whenReadTextReturnWith(expectedText: String) {
        every { textReader.readText() }.returns(expectedText.result())
    }

    private fun whenReadTextReportIOExceptionToCaller() {
        every { textReader.readText() }.returns(IOException().failure())
    }

    @Nested
    inner class FunctionalStyleHandling {

        @Test
        fun handle_error_explicit() {
            whenReadTextReportIOExceptionToCaller()
            val r = textReader.readText()
            assertThat(r.getErrorOrNull()).all {
                isNotNull()
                given { actual -> actual is IOException }
            }
        }

        @Test
        fun get_result_of_text_using_fold_if_exists() {
            val expectedText = "expectedText"
            whenReadTextReturnWith(expectedText)
            val r: String = textReader.readText().fold({ throw it }, { it })
            assertThat(r).isEqualTo(expectedText)
        }

        @Test
        fun get_result_of_text_using_getOr_if_exists() {
            val expectedText = "expectedText"
            whenReadTextReturnWith(expectedText)
            val r: String = textReader.readText().getOr { "notFound" }
            assertThat(r).isEqualTo(expectedText)
        }


        @Test
        fun get_result_of_text_using_getOrNull_if_exists() {
            val expectedText = "expectedText"
            whenReadTextReturnWith(expectedText)
            assertThat(textReader.readText().getOrNull()).all {
                isNotNull()
                isEqualTo(expectedText)
            }
        }

        @Test
        fun get_result_of_text_reader_should_fail_with_exception_on_left_case() {
            whenReadTextReportIOExceptionToCaller()
            val r = textReader.readText().fold({ it }, { it })
            assertThat(r).isInstanceOf(IOException::class)
        }
    }

    @Nested
    internal inner class FunctionalProcessing {

        @Test
        fun map_then_fold() {
            whenReadTextReturnWith("101")
            val i = textReader.readText().map { it.toInt() }.fold({0},{it})
            assertThat(i).isEqualTo(101)
        }

        @Test
        fun fold() {
            val expected = 101
            whenReadTextReturnWith("$expected")
            val actual = textReader.readText().fold({0},{it.toInt()})
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun fold_failure() {
            whenReadTextReportIOExceptionToCaller()
            val errorIndicator = -1
            val actual = textReader.readText().fold({errorIndicator},{it.toInt()})
            assertThat(actual).isEqualTo(errorIndicator)

        }
    }

    @MockK(relaxed = true)
    private lateinit var textReader: TextReader
}






