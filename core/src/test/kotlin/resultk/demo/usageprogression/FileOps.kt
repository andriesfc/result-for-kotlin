@file:JvmName("FileOps")

package resultk.demo.usageprogression

import resultk.*
import resultk.demo.usageprogression.FileRequirementError.UnexpectedIOFailure
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

fun File.size(): Result<IOException, Long> = resultOf {

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

fun File.found(): Result<IOException, File> = resultOf {
    if (!exists()) {
        throw FileNotFoundException(path)
    }
    success()
}

sealed class FileRequirementError {
    data class ExistsAlreadyAs(val kind: FileKind) : FileRequirementError()
    data class ParentPathDoesNotExists(val path: String) : FileRequirementError()
    data class UnexpectedIOFailure(
        val kind: FileKind,
        val path: String,
        val cause: IOException
    ) : FileRequirementError(), ThrowableProvider<IOException> by ThrowableProvider.of(cause)
}

fun File.create(
    ofKind: FileKind.Known,
    includeParents: Boolean = false
): Result<IOException, File> = resultOf {

    val existAsKindAlready = kind().orNull()
    if (existAsKindAlready != null) {
        if (existAsKindAlready == kind()) {
            return success()
        } else throw IOException(
            "Unable to create a $ofKind as it already exists a ${existAsKindAlready.name} here: $path"
        )
    }

    if (!parentFile.exists() && !includeParents) {
        throw IOException("Unable to create directory named $name as the parent path does not exists: $parent")
    }

    when (ofKind) {
        FileKind.Directory -> if (!mkdirs()) {
            throw IOException("Unable to create directory: $path")
        }
        FileKind.RegularFile -> if (!createNewFile()) {
            throw IOException("Unable to create regular file: $path")
        }
    }

    success()
}

fun File.required(
    kind: FileKind.Known,
    includeParents: Boolean = true,
    createIfNotExists: Boolean = true
): Result<FileRequirementError, File> {

    if (exists()) {
        return when (val actualKind = kind().or(FileKind.Unknown)) {
            kind -> success()
            else -> FileRequirementError.ExistsAlreadyAs(actualKind).failure()
        }
    }

    if (!parentFile.exists() && !includeParents) {
        return FileRequirementError.ParentPathDoesNotExists(path).failure()
    }

    val fileSystemError = fun(ex: IOException): UnexpectedIOFailure {
        return UnexpectedIOFailure(
            kind,
            path,
            ex
        )
    }

    return resultWithHandlingOf(fileSystemError) {
        if (!createIfNotExists) {
            throw FileNotFoundException(
                "Required ${kind.name} does not exists: $path"
            )
        }
        when (kind) {
            FileKind.Directory -> if (!mkdirs()) {
                throw IOException("Unable to create directory: $path")
            }
            FileKind.RegularFile -> if (!createNewFile()) {
                throw IOException("Unable to cearte regular file: $path")
            }
        }
        success()
    }
}

sealed class FileKind(val name: String) {

    object Directory : Known("directory")
    object RegularFile : Known("regular_file")
    object Unknown : FileKind("unknown")

    override fun toString(): String = name

    sealed class Known(name: String) : FileKind(name) {
        companion object {
            fun kinds() = Known::class.sealedSubclasses.mapNotNull { it.objectInstance }
        }
    }

}
