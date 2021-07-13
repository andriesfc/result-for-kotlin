package resultk.demo.filesysops

import resultk.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

fun File.size(): Result<IOException, Long> = result {

    if (!exists()) {
        throw FileNotFoundException(path)
    }

    if (!isFile) {
        throw IOException("Expected regular file: $path")
    }

    length().success()
}

fun File.kind(): Result<IOException, FileKind> = found().map {
    when {
        isDirectory -> FileKind.Directory
        isFile -> FileKind.RegularFile
        else -> FileKind.Unknown
    }
}

fun File.found(): Result<IOException, File> = result {
    if (!this.exists()) {
        throw FileNotFoundException(path)
    }
    success()
}

fun File.create(kind: FileKind.Known, includeParents: Boolean = false): Result<IOException, File> = result {

    kind().takeSuccessIf { it != kind }?.also { existAlreadyAsDifferentKind ->
        throw IOException(
            "Unable to create a $kind as it already exists a ${existAlreadyAsDifferentKind.value.name} here: $path"
        )
    }

    if (exists()) {
        return success()
    }

    if (!parentFile.exists() && !includeParents) {
        throw IOException("Unable to create directory named $name as the parent path does not exists: $parent")
    }

    when (kind) {
        FileKind.Directory -> if (!mkdirs()) {
            throw IOException("Unable to create directory: $path")
        }
        FileKind.RegularFile -> if (!createNewFile()) {
            throw IOException("Unable to create regular file: $path")
        }
    }

    success()
}


sealed class FileKind(val name: String) {
    override fun toString(): String = name
    sealed class Known(name: String) : FileKind(name) {
        companion object {
            fun values() = Known::class.sealedSubclasses.mapNotNull { it.objectInstance }
        }
    }

    object Directory : Known("directory")
    object RegularFile : Known("regular_file")
    object Unknown : FileKind("unknown")
}
