package resultk.testing

import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.security.MessageDigest

inline fun <reified C> classpathFile(resource: String): File = C::class.java.getResource(resource)
    ?.toURI()
    ?.let(Path::of)
    ?.toFile()
    ?: throw IOException("Unable to resolve classpath resource to regular file: $resource")


fun MessageDigest.update(file: File) {
    file.forEachBlock { input, bytes -> update(input, 0, bytes) }
}