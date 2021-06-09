package io.github.andriesfc.resultk

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.EOFException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketException
import java.util.*
import java.util.UUID.randomUUID
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail


@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResultTests {

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
        every { textReader.readText() }.returns(expectedText.success())
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
            val r: String? = textReader.readText().fold({ null }, { it })
            assertThat(r).all {
                isNotNull()
                isEqualTo(expectedText)
            }
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
            val (e, code) = error
            assertThat(e).isInstanceOf(IOException::class.java)
            assertThat(e.message).isEqualTo("bang!")
            assertThat(code).isEqualTo(ErrorCode.GeneralIOError)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "bad_price,@null",
                "@null,69.69"],
            nullValues = ["@null"]
        )
        fun transpose_failure_with_success(failureCode: String?, successPrice: Double?) {

            val given = failureCode?.failure()
                ?: successPrice?.success()
                ?: throw IllegalArgumentException("Both failureCode and successPrice cannot be null")

            val (transposedValue, transposedError) = given.transpose()

            if (failureCode != null) {
                assertThat(transposedValue.get()).isEqualTo(failureCode)
            } else if (successPrice != null) {
                assertThat(transposedError).isEqualTo(successPrice)
            }
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = ["@nothing"],
            value = [
                "12,@nothing",
                "@nothing, bean_counter_offline"
            ]
        )
        fun consuming_both_success_and_possible_failures(beansCounted: Int?, beanCountingErrorCode: String?) {

            val given: Result<String, Int> =
                beansCounted?.success()
                    ?: beanCountingErrorCode?.failure()
                    ?: throw IllegalArgumentException()

            lateinit var consumeValue: Any
            var isOnSuccess: Boolean? = null

            given
                .onSuccess { consumeValue = it; isOnSuccess = true }
                .onFailure { consumeValue = it; isOnSuccess = false }

            when (isOnSuccess) {
                true -> assertThat(consumeValue, "onSuccess{}").isEqualTo(beansCounted)
                false -> assertThat(consumeValue, "onFailure{}").isEqualTo(beanCountingErrorCode)
                else -> fail("Neither onSuccess nor onFailure was called")
            }
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = ["@nothing"],
            value = [
                "12,@nothing",
                "@nothing, bean_counter_offline"
            ]
        )
        fun use_resultOf_function_to_compose_result(beansCounted: Int?, beanCountingErrorCode: String?) {

            class BeanCountingException(val errorCode: String) : Exception(errorCode)

            val given = resultOf<BeanCountingException, Int> {
                when {
                    beansCounted != null -> beansCounted.success()
                    beanCountingErrorCode != null -> throw BeanCountingException(beanCountingErrorCode)
                    else -> throw IllegalArgumentException()
                }
            }

            val (counter, countingError) = given

            when {
                beanCountingErrorCode != null -> {
                    assertNotNull(countingError)
                    assertThat(countingError::errorCode).isEqualTo(beanCountingErrorCode)
                    assertThat(countingError).hasMessage(beanCountingErrorCode)
                }
                beansCounted != null -> assertThat(counter.get()).isEqualTo(beansCounted)
            }
        }


        @ParameterizedTest
        @CsvSource(
            nullValues = ["@nothing"],
            value = [
                "12,@nothing",
                "@nothing, bean_counter_offline"
            ]
        )
        fun toOptional_is_correct_for_all_cases(beansCounted: Int?, beanCountingErrorCode: String?) {

            val given: Result<String, Int> = when {
                beansCounted != null -> beansCounted.success()
                beanCountingErrorCode != null -> beanCountingErrorCode.failure()
                else -> throw IllegalArgumentException(
                    "Both beansCounted and beanCountedErrors cannot be set to null."
                )
            }

            val optional: Optional<Int> = given.toOptional()

            when {
                beanCountingErrorCode != null -> {
                    assertTrue(optional.isEmpty)
                }
                beansCounted != null -> {
                    assertTrue(optional.isPresent)
                    assertThat(optional.get()).isEqualTo(beansCounted)
                }
            }
        }

        @ParameterizedTest
        @CsvSource(
            nullValues = ["@nothing"],
            value = [
                "12",
                "@nothing"
            ]
        )
        fun convert_optional_to_proper_result(beansCounted: Int?) {

            val given = Optional.ofNullable(beansCounted)
            val (counted, counterError) = given.toResult { "unknown_bean_counting_error" }

            when (beansCounted) {
                null -> {
                    assertThat(counterError).isEqualTo("unknown_bean_counting_error")
                    assertThrows<UnhandledFailureException> { counted.get() }
                }
                else -> {
                    assertThat(counted.get()).isEqualTo(beansCounted)
                    assertThat(counterError).isNull()
                }
            }
        }
    }

    @Nested
    internal inner class LetsComputeSomeFileSizesTest {

        @TempDir
        internal lateinit var workDir: File
        private lateinit var file: File

        @BeforeEach
        fun prepare() {
            file = File(workDir, "test-${randomUUID()}.dat")
        }

        @Test
        fun lets_caught_file_not_found_on_missing_file() {
            val (size, ex) = preparedFileSize()
            assertAll(
                { assertThrows<FileNotFoundException> { size.get() } },
                { assertTrue { ex is FileNotFoundException } }
            )
        }

        @Test
        fun lets_return_file_size_if_found() {
            assertThat(file.createNewFile()).isTrue()
            val expectedByteSize = Random.nextInt(10..200)
            file.writeBytes(Random.nextBytes(expectedByteSize))
            val (fileSizeInBytes) = preparedFileSize().map(Long::toInt)
            assertDoesNotThrow { fileSizeInBytes.get() }
            assertThat(fileSizeInBytes.get(), "fileSizeInBytes.get()").isEqualTo(expectedByteSize)
        }

        private fun preparedFileSize(): Result<IOException, Long> {
            return file.computeResultWith {
                if (exists()) {
                    length().success()
                } else {
                    throw FileNotFoundException("$this")
                }
            }
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



