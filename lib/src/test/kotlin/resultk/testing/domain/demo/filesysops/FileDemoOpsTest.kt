package resultk.testing.domain.demo.filesysops

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.testing.assertions.isFailureResult
import resultk.testing.assertions.isSuccessResult
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Tests demo file operations")
internal class FileDemoOpsTest {

    @TempDir
    lateinit var parentDir: File
    private lateinit var path: File

    @Test
    fun `Test file size should fail on non existing file`() {
        path = File(parentDir, "someFile.txt")
        assertThat(path.size(), "file.size()").isFailureResult()
            .isInstanceOf(FileNotFoundException::class)
            .hasMessage(path.path)
    }

    @Test
    fun `Test file size should fail if file is directory`() {
        path = File(parentDir, "someDirectory").apply { mkdir() }
        assertThat(path, "path").exists()
        assertThat(path.size(), "path.size()").isFailureResult().isInstanceOf(IOException::class.java)
    }

    @Test
    fun `Test found on existing file`() {
        path = File(parentDir, "blob-20221d9b-d625-48f2-bad7-c6904e0d981a.txt").apply { createNewFile() }
        assertThat(path, "path").exists()
        assertThat(path.found(), "path.found()").isSuccessResult().isEqualTo(path)
    }

    @ParameterizedTest
    @MethodSource("knownFileKinds")
    fun `Test create file`(kind: FileKind.Known) {
        path = File(parentDir, "${kind.name}-${UUID.randomUUID()}")
        println(path)
        val created = path.create(kind)
        assertThat(created, "path.create($kind)").isSuccessResult().all {
            exists()
            isEqualTo(path)
            when (kind) {
                FileKind.Directory -> isDirectory()
                FileKind.RegularFile -> isFile()
            }
        }
    }

    @Test
    fun `Test create directory including parents`() {
        path = File(parentDir, "/todos/2020/12/16")
        val created = path.create(FileKind.Directory, includeParents = true)
        println("created: $created")
        assertThat(created).isSuccessResult().isEqualTo(path)
    }


    @ParameterizedTest
    @MethodSource("knownFileKinds")
    fun `Test kind of file`(kind: FileKind.Known) {

        path = File(parentDir, UUID.randomUUID().toString()).apply {
            when (kind) {
                FileKind.Directory -> mkdirs()
                FileKind.RegularFile -> createNewFile()
            }
        }

        assertThat(path).exists()
        assertThat(path.kind()).isSuccessResult().isEqualTo(kind)
    }

    fun knownFileKinds() = FileKind.Known.values()
}