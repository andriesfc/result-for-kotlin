package io.github.andriesfc.kotlin.result.demo

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import io.github.andriesfc.kotlin.result.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files.isRegularFile
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SampleErrorHandling {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun testFileSizeResultHandlingOnNonExistingFile() {
        val nonExistingFile = File(tempDir, "non-existing-file.txt")
        val fileSize = nonExistingFile.size()
        val fileSizeError = fileSize.errorOrEmpty()
        println(fileSize)
        assertThat(fileSizeError.isPresent).isTrue()
        assertThat(fileSizeError.get()).messageContains("$nonExistingFile")
    }

    @Test
    fun testFileSizeResultHandlingOnDirectory() {
        val dir = File(tempDir, "some-directory").makeDir()
        val fileSize = dir.flatmap(File::size)
        val fileSizeError = fileSize.errorOrEmpty()
        println(fileSize)
        assertThat(fileSizeError.isPresent).isTrue()
        assertThat(fileSizeError.get()).messageContains("Expected regular file")
    }

    @Test
    fun testGetFileSizeSucceeds() {
        val expectedFileSize = Random.nextInt(10, 1024)
        val nonEmptyFile = File(tempDir, "non-empty-file-with-$expectedFileSize-bytes").appendAnyBytes(expectedFileSize)
        val fileSize = nonEmptyFile.flatmap(File::size)
        val fileSizeError = fileSize.errorOrEmpty()
        println(nonEmptyFile)
        println(fileSize)
        assertThat(fileSizeError.isEmpty).isTrue()
        assertDoesNotThrow(fileSize::get)
        assertThat(fileSize.get()).isEqualTo(expectedFileSize.toLong())
    }

}

private fun File.makeDir(): Result<IOException, File> = result {

    val exists = exists()

    if (!exists) {
        if (!mkdir()) {
            throw IOException("Unable to create directory: $this")
        }
    } else if (!isDirectory) {
        throw IOException("File exists already, but not as a directory: $this")
    }

    success()
}


private fun File.size(): Result<IOException, Long> = result {

    if (!exists()) {
        throw FileNotFoundException("$this")
    }

    if (!isRegularFile(toPath())) {
        throw IOException("Expected regular file here: $this")
    }

    length().success()
}

private fun File.appendAnyBytes(expectedSize: Int): Result<IOException, File> = result {

    require(expectedSize >= 0) {
        "Expected file size needs to be zero or more: $expectedSize"
    }

    if (!exists()) {
        if (!createNewFile()) {
            throw IOException("Unable to create new file: $this")
        }
    } else if (!isFile) {
        throw IOException("Unable to write to non regular file: $this")
    }

    writeBytes(Random.nextBytes(expectedSize))
    success()
}