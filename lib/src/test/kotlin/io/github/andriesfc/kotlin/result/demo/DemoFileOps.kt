package io.github.andriesfc.kotlin.result.demo

import io.github.andriesfc.kotlin.result.Result
import io.github.andriesfc.kotlin.result.result
import io.github.andriesfc.kotlin.result.success
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import kotlin.random.Random

fun File.makeDir(): Result<IOException, File> = result {

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

fun File.fileSize(): Result<IOException, Long> = result {

    if (!exists()) {
        throw FileNotFoundException("$this")
    }

    if (!Files.isRegularFile(toPath())) {
        throw IOException("Expected regular file here: $this")
    }

    length().success()
}

fun File.appendAnyBytes(expectedSize: Int): Result<IOException, File> = result {

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