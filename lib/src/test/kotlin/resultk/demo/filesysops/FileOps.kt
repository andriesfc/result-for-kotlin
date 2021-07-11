package resultk.demo.filesysops

import resultk.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

fun File.size(): Result<IOException, Long> = found().map(File::length)
fun File.existingKind(): Result<IOException, FileKind> = found().map {
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

fun File.create(kind: FileKind.Known, includeParents: Boolean = false): Result<IOException, File> {

    val existAlreadyAsDifferentKind = existingKind().takeValueIf { it != kind }?.value
    if (existAlreadyAsDifferentKind != null) {
        throw IOException(
            "Unable to create a $kind as it already exists a ${existAlreadyAsDifferentKind.name} here: $path"
        )
    }

    if (exists()) {
        return success()
    }

    if (!parentFile.exists()) {
        if (!includeParents) {
            throw IOException("Unable to create ${kind.name} as the parent folder does not exist here: $parent")
        }
        if (!mkdirs()) {
            throw IOException("Unable to create parent folder for $this")
        }
    }

    val created = when (kind) {
        FileKind.Directory -> mkdir()
        FileKind.RegularFile -> createNewFile()
    }

    if (!created) {
        throw IOException("Unable to create ${kind.name} here: $path")
    }

    return success()
}

sealed class FileKind(val name: String) {
    override fun toString(): String = name
    sealed class Known(name: String) : FileKind(name)
    object Directory : Known("directory")
    object RegularFile : Known("regular_file")
    object Unknown : FileKind("unknown")
}