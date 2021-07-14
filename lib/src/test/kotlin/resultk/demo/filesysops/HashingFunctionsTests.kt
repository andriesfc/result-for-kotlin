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
import resultk.onSuccess
import resultk.testing.assertions.isFailure
import resultk.testing.assertions.isSuccess
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
    @DisplayName("Hash function produce the expected hash.")
    fun testHashingFunctionsProduceSameResult(testArgs: FileHashFunctionTestArgs) {
        val (funcName, func) = testArgs
        val actual = knownFile.func(hashAlgorithm).onSuccess { println("${knownFile}.$funcName = $it") }
        assertThat(actual).isSuccess().isEqualTo(knownFileSh1Hash)
    }

    @ParameterizedTest
    @MethodSource("hashingFunctions")
    @DisplayName("Hash function is able to handle non existent file.")
    fun testHashingFunctionsHandleReadErrorTheSame(testArgs: FileHashFunctionTestArgs) {
        val (_,func) = testArgs
        val contentHash = nonExistingFile.func(hashAlgorithm).also(::println)
        assertThat(contentHash).isFailure().all {
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
    @DisplayName("Hash function should be able report on unsupported hash algorithm.")
    fun testHashingFunctionsHandleFailsWithNonExistingHashFunction(testArgs: FileHashFunctionTestArgs) {
        val (_,func) = testArgs
        val actual = nonExistingFile.func(nonExistingHashingAlgorithm).also(::println)
        assertThat(actual).isFailure().given { error ->
            assertThat(error.cause).isInstanceOf(NoSuchAlgorithmException::class)
            assertThat(error).isInstanceOf(HashingError.UnsuportedAlgorithm::class)
            assertThat((error as HashingError.UnsuportedAlgorithm).algorithm).isEqualTo(
                nonExistingHashingAlgorithm
            )
        }
    }

    fun hashingFunctions(): List<FileHashFunctionTestArgs> = listOf(
        File::hashContentsV0,
        File::hashContentsV1,
        File::hashContentsV2,
        File::hashContentsV3,
        File::hashContentsV4,
        File::hashContentsV5
    ).map { function -> function.name to function }
}

private typealias FunctionName = String
private typealias FileHashFunction = File.(String) -> Result<HashingError<Exception>, String>
private typealias FileHashFunctionTestArgs = Pair<FunctionName, FileHashFunction>