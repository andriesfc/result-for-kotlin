package resultk.demo.filesysops

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.testing.assertions.isFailure
import resultk.testing.assertions.isSuccess
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
    fun test_file_size_should_fail_on_non_existing_file() {
        path = File(parentDir, "someFile.txt")
        assertThat(path.size(), "file.size()").isFailure()
            .isInstanceOf(FileNotFoundException::class)
            .hasMessage(path.path)
    }

    @Test
    fun test_file_size_should_fail_if_file_is_directory() {
        path = File(parentDir, "someDirectory").apply { mkdir() }
        assertThat(path, "path").exists()
        assertThat(path.size(), "path.size()").isFailure().isInstanceOf(IOException::class.java)
    }

    @Test
    fun test_found_on_existing_file() {
        path = File(parentDir, "blob-20221d9b-d625-48f2-bad7-c6904e0d981a.txt").apply { createNewFile() }
        assertThat(path, "path").exists()
        assertThat(path.found(), "path.found()").isSuccess().isEqualTo(path)
    }

    @ParameterizedTest
    @MethodSource("knownFileKinds")
    fun test_create_file(kind: FileKind.Known) {
        path = File(parentDir, "${kind.name}-${UUID.randomUUID()}")
        println(path)
        val created = path.create(kind)
        assertThat(created, "path.create($kind)").isSuccess().all {
            exists()
            isEqualTo(path)
            when (kind) {
                FileKind.Directory -> isDirectory()
                FileKind.RegularFile -> isFile()
            }
        }
    }

    @Test
    fun test_create_directory_including_parents() {
        path = File(parentDir, "/todos/2020/12/16")
        val created = path.create(FileKind.Directory, includeParents = true)
        println("created: $created")
        assertThat(created).isSuccess().isEqualTo(path)
    }


    @ParameterizedTest
    @MethodSource("knownFileKinds")
    fun test_kind_of_file(kind: FileKind.Known) {

        path = File(parentDir, UUID.randomUUID().toString()).apply {
            when (kind) {
                FileKind.Directory -> mkdirs()
                FileKind.RegularFile -> createNewFile()
            }
        }

        assertThat(path).exists()
        assertThat(path.kind()).isSuccess().isEqualTo(kind)
    }

    fun knownFileKinds() = FileKind.Known.values()
}