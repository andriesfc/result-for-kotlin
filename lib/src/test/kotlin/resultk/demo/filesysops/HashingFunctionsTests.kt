package resultk.demo.filesysops

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.apache.commons.codec.binary.Hex.encodeHexString
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.Result
import resultk.assertions.error
import resultk.assertions.value
import resultk.onSuccess
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Paths
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.random.Random


/**
 * This test verifies that all he different versions of sample file hash functions behaves exactly the same manner.
 *
 * @see File.hashContentsV0
 * @see File.hashContentsV1
 * @see File.hashContentsV2
 * @see File.hashContentsV3
 * @see File.hashContentsV4
 * @see File.hashContentsV5
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Verifies that all he different versions of sample file hash functions behaves exactly the same manner")
internal class HashingFunctionsTests {

    private lateinit var knownFileSh1Hash: String
    private lateinit var knownFile: File
    private lateinit var nonExistingFile: File
    private lateinit var nonExistingHashingAlgorithm: String
    private lateinit var hashAlgorithm: String

    @BeforeAll
    fun setUpAll() {

        hashAlgorithm = "sha1"

        knownFile = javaClass.getResource("text")?.run {
            Paths.get(toURI()).toFile()
        } ?: throw IOException("Expected text resource not find as regular file on the classpath.")

        knownFileSh1Hash = MessageDigest.getInstance("sha1").run {
            knownFile.forEachBlock { buffer, bytesRead -> if (bytesRead > 0) update(buffer, 0, bytesRead) }
            encodeHexString(digest())
        }

        nonExistingFile = File("file-i-do-not-exists-${UUID.randomUUID()}")
        nonExistingHashingAlgorithm = "sha-i-do-not-exists-${Random.nextLong().toString(16)}"
    }

    @ParameterizedTest
    @MethodSource("hashingFunctions")
    @DisplayName("All hash functions produce the same results.")
    fun testHashingFunctionsProduceSameResult(func: File.(String) -> Result<HashingError<Exception>, String>) {
        val actual = func(knownFile, hashAlgorithm).onSuccess { println("$hashAlgorithm($knownFile) = $it") }
        assertThat(actual).value().isEqualTo(knownFileSh1Hash)
    }

    @ParameterizedTest
    @MethodSource("hashingFunctions")
    @DisplayName("All hash functions are able to handle read errors.")
    fun testHashingFunctionsHandleReadErrorTheSame(func: File.(String) -> Result<HashingError<Exception>, String>) {
        val contentHash = func(nonExistingFile, hashAlgorithm).also(::println)
        assertThat(contentHash).error().all {
            given { actual ->
                assertThat(actual).isInstanceOf(HashingError.SourceContentNotReadable::class)
                (actual as HashingError.SourceContentNotReadable)
                transform { actual.source }.isEqualTo(nonExistingFile.path)
                transform { actual.cause }.isInstanceOf(FileNotFoundException::class)
            }
        }
    }


    @ParameterizedTest
    @MethodSource("hashingFunctions")
    @DisplayName("All hash functions are able to deal with unsupported hash algorithms.")
    fun testHashingFunctionsHandleFailsWithNonExistingHashFunction(func: File.(String) -> Result<HashingError<Exception>, String>) {
        val actual = func(nonExistingFile, nonExistingHashingAlgorithm).also(::println)
        assertThat(actual).error().given { error ->
            assertThat(error.cause).isInstanceOf(NoSuchAlgorithmException::class)
            assertThat(error).isInstanceOf(HashingError.UnsuportedAlgorithm::class)
            assertThat((error as HashingError.UnsuportedAlgorithm).algorithm).isEqualTo(
                nonExistingHashingAlgorithm
            )
        }
    }

    fun hashingFunctions() = listOf(
        File::hashContentsV0,
        File::hashContentsV1,
        File::hashContentsV2,
        File::hashContentsV3,
        File::hashContentsV4,
        File::hashContentsV5
    )
}