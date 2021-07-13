package resultk.demo.filesysops

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.Result
import resultk.assertions.error
import resultk.assertions.value
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths
import java.security.NoSuchAlgorithmException
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HashingFunctionsTests {

    private lateinit var knownFile: File
    private lateinit var nonExistingFile: File
    private val expectedSha1Binary = "b314c7ebb7d599944981908b7f3ed33a30e78f3a"
    private val nonExistingHashingAlgorithm = "shb596_632_1"

    @BeforeAll
    fun setUpAll() {
        knownFile = javaClass.getResource("text")?.toURI()
            ?.let(Paths::get)?.toFile()
            ?: throw FileNotFoundException()

        nonExistingFile = File("i-do-not-exists-${UUID.randomUUID()}")
    }

    @ParameterizedTest
    @MethodSource("hashingFunctions")
    fun testHashingFunctionsProduceSameResult(func: File.(String) -> Result<HashingError<Exception>, String>) {
        val actual = func(knownFile, "sha1")
        assertThat(actual).value().isEqualTo(expectedSha1Binary)
    }

    @ParameterizedTest
    @MethodSource("hashingFunctions")
    fun testHashingFunctionsHandleReadErrorTheSame(func: File.(String) -> Result<HashingError<Exception>, String>) {
        val actual = func(nonExistingFile, "sha1").also(::println)
        assertThat(actual).error().given { error ->
            assertThat(error.cause).isInstanceOf(FileNotFoundException::class)
            assertThat(error).isInstanceOf(HashingError.IOError::class)
            assertThat((error as HashingError.IOError).source).isEqualTo(nonExistingFile.path)
        }
    }


    @ParameterizedTest
    @MethodSource("hashingFunctions")
    fun testHashingFunctionsHandleFailsWithNonExistingHashFunction(func: File.(String) -> Result<HashingError<Exception>, String>) {
        val actual = func(nonExistingFile, nonExistingHashingAlgorithm).also(::println)
        assertThat(actual).error().given { error ->
            assertThat(error.cause).isInstanceOf(NoSuchAlgorithmException::class)
            assertThat(error).isInstanceOf(HashingError.DigesterAlgorithmNotSupported::class)
            assertThat((error as HashingError.DigesterAlgorithmNotSupported).algorithm).isEqualTo(
                nonExistingHashingAlgorithm
            )
        }
    }

    fun hashingFunctions() = listOf(
        File::hashContentsV1,
        File::hashContentsV2,
        File::hashContentsV3,
        File::hashContentsV4,
        File::hashContentsV5
    )
}