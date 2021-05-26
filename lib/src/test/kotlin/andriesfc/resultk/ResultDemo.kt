package andriesfc.resultk

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import java.util.UUID.randomUUID
import kotlin.test.assertNotNull


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
            whenReadTextReportIOExceptionToCaller("kaboom")
            val (text, e) = textReader.readText()
            assertNotNull(e)
            assertThat(e).isInstanceOf(IOException::class.java)
            assertThat(e.message).isEqualTo("kaboom")
            val thrown = assertThrows<IOException>(text::get)
            assertThat(thrown::message).isEqualTo("kaboom")
        }

        @Test
        fun handle_error_with_try_catch() {
            whenReadTextReportIOExceptionToCaller("bang!")
            val thrown = assertThrows<IOException> { textReader.readText().get() }
            assertThat(thrown::message).isEqualTo("bang!")
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

    private fun whenReadTextReportIOExceptionToCaller(message: String? = null) {
        every { textReader.readText() }.returns((message?.let(::IOException) ?: IOException()).failure())
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
            val i = textReader.readText().map { it.toInt() }.fold({ 0 }, { it })
            assertThat(i).isEqualTo(101)
        }

        @Test
        fun fold() {
            val expected = 101
            whenReadTextReturnWith("$expected")
            val actual = textReader.readText().fold({ 0 }, { it.toInt() })
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun fold_failure() {
            whenReadTextReportIOExceptionToCaller()
            val errorIndicator = -1
            val actual = textReader.readText().fold({ errorIndicator }, { it.toInt() })
            assertThat(actual).isEqualTo(errorIndicator)
        }

        @Test
        fun map_value() {
            val expected = 6071
            whenReadTextReturnWith("$expected")
            val (n, _) = textReader.readText().map { it.toInt() }
            assertThat(n.get()).isEqualTo(expected)
        }

        @Test
        fun map_IOException_to_enum() {
            whenReadTextReportIOExceptionToCaller("bang!")
            val (_, error) = textReader.readText().mapFailure { ErrorCode.of(it) }
            assertNotNull(error)
            val (e,code) = error
            assertThat(e).isInstanceOf(IOException::class.java)
            assertThat(e.message).isEqualTo("bang!")
            assertThat(code).isEqualTo(ErrorCode.GeneralIOError)
        }

    }

    private enum class ErrorCode {
        EndOfFile,
        GeneralIOError,
        RemoteException;
        companion object {
            fun of(e: IOException): Pair<IOException, ErrorCode> {
                return e to when (e) {
                    is SocketException -> RemoteException
                    is EOFException -> EndOfFile
                    else -> GeneralIOError
                }
            }
        }
    }

    @MockK(relaxed = true)
    private lateinit var textReader: TextReader
}



