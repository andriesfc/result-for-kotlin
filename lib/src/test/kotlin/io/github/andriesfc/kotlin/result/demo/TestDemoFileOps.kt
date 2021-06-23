package io.github.andriesfc.kotlin.result.demo

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import io.github.andriesfc.kotlin.result.errorOrEmpty
import io.github.andriesfc.kotlin.result.flatmap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestDemoFileOps {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun testFileSizeResultHandlingOnNonExistingFile() {
        val nonExistingFile = File(tempDir, "non-existing-file.txt")
        val fileSize = nonExistingFile.fileSize()
        val fileSizeError = fileSize.errorOrEmpty()
        println(fileSize)
        assertThat(fileSizeError.isPresent).isTrue()
        assertThat(fileSizeError.get()).messageContains("$nonExistingFile")
    }

    @Test
    fun testFileSizeResultHandlingOnDirectory() {
        val dir = File(tempDir, "some-directory").makeDir()
        val fileSize = dir.flatmap(File::fileSize)
        val fileSizeError = fileSize.errorOrEmpty()
        println(fileSize)
        assertThat(fileSizeError.isPresent).isTrue()
        assertThat(fileSizeError.get()).messageContains("Expected regular file")
    }

    @Test
    fun testGetFileSizeSucceeds() {
        val expectedFileSize = Random.nextInt(10, 1024)
        val nonEmptyFile = File(tempDir, "non-empty-file-with-$expectedFileSize-bytes").appendAnyBytes(expectedFileSize)
        val fileSize = nonEmptyFile.flatmap(File::fileSize)
        val fileSizeError = fileSize.errorOrEmpty()
        println(nonEmptyFile)
        println(fileSize)
        assertThat(fileSizeError.isEmpty).isTrue()
        assertDoesNotThrow(fileSize::get)
        assertThat(fileSize.get()).isEqualTo(expectedFileSize.toLong())
    }

}

