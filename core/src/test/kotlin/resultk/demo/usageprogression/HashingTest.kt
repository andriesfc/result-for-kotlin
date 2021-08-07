package resultk.demo.usageprogression

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import org.apache.commons.codec.binary.Hex.encodeHexString
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.Result
import resultk.demo.domain.HashingError
import resultk.demo.domain.HashingError.SourceContentNotReadable
import resultk.onSuccess
import resultk.testing.assertions.isFailureResult
import resultk.testing.assertions.isSuccessResult
import resultk.testing.classpathFile
import resultk.testing.update
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
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
        knownFile = classpathFile<HashingFunctionsTests>("text")
        knownFileSh1Hash = MessageDigest.getInstance("sha1").let { digest ->
            digest.update(knownFile)
            encodeHexString(digest.digest())
        }

        nonExistingFile = File("file-i-do-not-exists-${UUID.randomUUID()}")
        nonExistingHashingAlgorithm = "sha-i-do-not-exists-${Random.nextLong().toString(16)}"
    }

    @ParameterizedTest
    @MethodSource("hashingFunctions")
    fun `Test hashing functions produce same result`(testArgs: FileHashFunctionTestArgs) {
        val (funcName, func) = testArgs
        val actual = knownFile.func(hashAlgorithm).onSuccess { println("${knownFile}.$funcName = $it") }
        assertThat(actual).isSuccessResult().isEqualTo(knownFileSh1Hash)
    }

    @ParameterizedTest
    @MethodSource("hashingFunctions")
    fun `Test hashing functions handle read error the same`(testArgs: FileHashFunctionTestArgs) {
        val (_, func) = testArgs
        val contentHash = nonExistingFile.func(hashAlgorithm).also(::println)
        assertThat(contentHash)
            .isFailureResult()
            .isInstanceOf(SourceContentNotReadable::class).all {
                prop(SourceContentNotReadable::source).isEqualTo(nonExistingFile.path)
                prop(SourceContentNotReadable::cause).isInstanceOf(FileNotFoundException::class)
            }
    }


    @ParameterizedTest
    @MethodSource("hashingFunctions")
    fun `Test hashing functions handle fails with non existing hash function`(testArgs: FileHashFunctionTestArgs) {
        val (_, func) = testArgs
        val actual = nonExistingFile.func(nonExistingHashingAlgorithm).also(::println)
        assertThat(actual)
            .isFailureResult()
            .isInstanceOf(HashingError.UnsupportedAlgorithm::class).all {
                prop(HashingError.UnsupportedAlgorithm::algorithm).isEqualTo(nonExistingHashingAlgorithm)
            }
    }

    fun hashingFunctions(): List<FileHashFunctionTestArgs> = listOf(
        File::hashContentsV0,
        File::hashContentsV1,
        File::hashContentsV2,
        File::hashContentsV3,
        File::hashContentsV4
    ).map { function -> function.name to function }
}

private typealias FunctionName = String
private typealias FileHashFunction = File.(String) -> Result<HashingError<Exception>, String>
private typealias FileHashFunctionTestArgs = Pair<FunctionName, FileHashFunction>