package resultk

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith


@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Functional operations on the Result<E,T> type")
internal class FunctionalResultTests {




/*
    private interface TextReader {
        fun readText(): Result<IOException, String>
    }

    @MockK(relaxed = true)
    private lateinit var textReader: TextReader

    @TempDir
    internal lateinit var workDir: File

    private lateinit var file: File

    @BeforeEach
    fun prepare() {
        file = File(workDir, "test-${randomUUID()}.dat")
    }

    @Test
    fun result_does_not_handle_unexpected_exception() {
        val expectedClassCastMessage = "Class cast message!"
        val expected = assertThrows<ClassCastException> { result<String,Int> { throw ClassCastException(expectedClassCastMessage) } }
        assertEquals(expectedClassCastMessage, expected.message)
    }

    @Test
    fun handle_error_explicit_idiomatic() {
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
        assertThat(textReader.readText().get()).isEqualTo(expectedText)
    }


    @Test
    fun handle_error_explicit() {
        whenReadTextReportIOExceptionToCaller()
        val r = textReader.readText()
        assertThat(r.errorOrNull()).all {
            isNotNull()
            given { actual -> actual is IOException }
        }
    }

    @Test
    fun get_result_of_text_using_fold_if_exists() {
        val expectedText = "expectedText"
        whenReadTextReturnWith(expectedText)
        val r: String? = textReader.readText().map({ null }, { it })
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
        val r = textReader.readText().map({ it }, { it })
        assertThat(r).isInstanceOf(IOException::class)
    }


    @Test
    fun map_then_fold() {
        whenReadTextReturnWith("101")
        val i = textReader.readText().map { it.toInt() }.map({ 0 }, { it })
        assertThat(i).isEqualTo(101)
    }

    @Test
    fun fold() {
        val expected = 101
        whenReadTextReturnWith("$expected")
        val actual = textReader.readText().map({ 0 }, { it.toInt() })
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun fold_failure() {
        whenReadTextReportIOExceptionToCaller()
        val errorIndicator = -1
        val actual = textReader.readText().map({ errorIndicator }, { it.toInt() })
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
    fun maps_IOException_to_enum() {
        whenReadTextReportIOExceptionToCaller("bang!")
        val (_, error) = textReader.readText().mapFailure { IOError.mappedBy(it) }
        assertNotNull(error)
        val (e, code) = error
        assertThat(e).isInstanceOf(IOException::class.java)
        assertThat(e.message).isEqualTo("bang!")
        assertThat(code).isEqualTo(IOError.GeneralIOError)
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
    fun use_result_function_to_compose_result(beansCounted: Int?, beanCountingErrorCode: String?) {

        class BeanCountingException(val errorCode: String) : Exception(errorCode)

        val given = result<BeanCountingException, Int> {
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

        val optional: Optional<Int> = given.optional()

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
        val (counted, counterError) = result(given) { "unknown_bean_counting_error" }

        when (beansCounted) {
            null -> {
                assertThat(counterError).isEqualTo("unknown_bean_counting_error")
                assertThrows<WrappedFailureAsException> { counted.get() }
            }
            else -> {
                assertThat(counted.get()).isEqualTo(beansCounted)
                assertThat(counterError).isNull()
            }
        }
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

    @Test
    fun lets_catch_throwable_on_result() {
        assertThat(file).doesNotExists()
        val (fileSize, ex) = file.resultCatching {
            when {
                exists() -> length().success()
                else -> throw FileNotFoundException(path)
            }
        }
        assertThat(ex).isNotNull().isInstanceOf(IOException::class)
        assertThat { fileSize.get() }.isFailure().isInstanceOf(IOException::class)
    }


    @Test
    fun lets_catch_throwable_and_build_proper_error_code() {
        assertThat(file).doesNotExists()
        val fileSize = file.resultCatching(IOError::of) {
            when {
                exists() -> length().success()
                else -> throw FileNotFoundException()
            }
        }
        println(fileSize)
        assertThat(fileSize).isFailure().isEqualTo(IOError.FileNotFound)
    }

    @Test
    fun using_try_catch_to_handle_error_code() {
        val r = result<IOError, Long> { IOError.GeneralIOError.failure() }
        assertThat { r.get() }
            .isFailure().isInstanceOf(Throwable::class.java)
            .transform { it.unwrapFailure<IOError>().error }
            .isEqualTo(IOError.GeneralIOError)
    }

    private fun preparedFileSize(): Result<IOException, Long> {
        return file.computeResult {
            if (exists()) {
                length().success()
            } else {
                throw FileNotFoundException("$this")
            }
        }
    }

    private fun whenReadTextReturnWith(expectedText: String) {
        every { textReader.readText() }.returns(expectedText.success())
    }

    private fun whenReadTextReportIOExceptionToCaller(message: String? = null) {
        every { textReader.readText() }.returns(
            when (message) {
                null -> IOException().failure()
                else -> IOException(message).failure()
            }
        )
    }

    private enum class IOError(
        private val mappedExceptionType: Class<out IOException>,
        private val priority: Int = 0
    ) {

        EndOfFile(EOFException::class.java),
        GeneralIOError(IOException::class.java, Int.MAX_VALUE),
        RemoteException(SocketException::class.java),
        FileNotFound(FileNotFoundException::class.java);

        companion object {

            private val mapped = values().sortedBy(IOError::priority)

            fun of(e: IOException): IOError {
                return mapped.firstOrNull { it.mappedExceptionType.isInstance(e) } ?: GeneralIOError
            }

            fun mappedBy(e: IOException): Pair<IOException, IOError> {
                return e to of(e)
            }
        }
    }*/
}



