package resultk.demo.filesysops

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.assertions.error
import resultk.assertions.value
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileOpsTest {

    @TempDir
    lateinit var parentDir: File
    private lateinit var path: File

    @Test
    fun testFileSizeShouldFailOnNonExistingFile() {
        path = File(parentDir, "someFile.txt")
        assertThat(path.size(), "file.size()").error()
            .isInstanceOf(FileNotFoundException::class)
            .hasMessage(path.path)
    }

    @Test
    fun testFileSizeShouldFailIfFileIsDirectory() {
        path = File(parentDir, "someDirectory").apply { mkdir() }
        assertThat(path, "path").exists()
        assertThat(path.size(), "path.size()").error().isInstanceOf(IOException::class.java)
    }

    @Test
    fun testFoundOnExistingFile() {
        path = File(parentDir, "blob-20221d9b-d625-48f2-bad7-c6904e0d981a.txt").apply { createNewFile() }
        assertThat(path, "path").exists()
        assertThat(path.found(), "path.found()").value().isEqualTo(path)
    }

    @ParameterizedTest
    @MethodSource("knownFileKinds")
    fun testCreateFile(kind: FileKind.Known) {
        path = File(parentDir, "${kind.name}-${UUID.randomUUID()}")
        println(path)
        val created = path.create(kind)
        assertThat(created, "path.create($kind)").value().all {
            exists()
            isEqualTo(path)
            when (kind) {
                FileKind.Directory -> isDirectory()
                FileKind.RegularFile -> isFile()
            }
        }
    }

    @Test
    fun testCreateDirectoryIncludingParents() {
        path = File(parentDir, "/todos/2020/12/16")
        val created = path.create(FileKind.Directory, includeParents = true)
        println("created: $created")
        assertThat(created).value().isEqualTo(path)
    }


    @ParameterizedTest
    @MethodSource("knownFileKinds")
    fun testKindOfFile(kind: FileKind.Known) {

        path = File(parentDir, UUID.randomUUID().toString()).apply {
            when (kind) {
                FileKind.Directory -> mkdirs()
                FileKind.RegularFile -> createNewFile()
            }
        }

        assertThat(path).exists()
        assertThat(path.kind()).value().isEqualTo(kind)
    }

    fun knownFileKinds() = FileKind.Known.values()
}