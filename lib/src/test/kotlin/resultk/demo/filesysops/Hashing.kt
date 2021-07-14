package resultk.demo.filesysops

import org.apache.commons.codec.binary.Hex.encodeHexString
import resultk.*
import resultk.demo.filesysops.HashingError.UnsuportedAlgorithm
import resultk.demo.filesysops.HashingError.SourceContentNotReadable
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

sealed class HashingError<out X : Exception> {

    abstract val cause: X

    data class SourceContentNotReadable(
        val source: String,
        override val cause: IOException
    ) : HashingError<IOException>()

    data class UnsuportedAlgorithm(
        override val cause: NoSuchAlgorithmException,
        val algorithm: String
    ) : HashingError<NoSuchAlgorithmException>()

}

fun File.hashContentsV0(algorithm: String): Result<HashingError<Exception>, String> {

    val messageDigest = try {
        MessageDigest.getInstance(algorithm)
    } catch (e: NoSuchAlgorithmException) {
        return UnsuportedAlgorithm(e, algorithm).failure()
    }

    val messageHash = try {
        forEachBlock { buffer, bytesRead ->
            if (bytesRead > 0) {
                messageDigest.update(buffer, 0, bytesRead)
            }
        }
        encodeHexString(messageDigest.digest())
    } catch (e: IOException) {
        return SourceContentNotReadable(path, e).failure()
    }

    return messageHash.success()
}

fun File.hashContentsV1(algorithm: String): Result<HashingError<Exception>, String> {
    return result {
        try {
            MessageDigest.getInstance(algorithm).run {
                forEachBlock { buffer, bytesRead ->
                    if (bytesRead > 0) {
                        update(buffer, 0, bytesRead)
                    }
                }
                encodeHexString(digest()).success()
            }
        } catch (e: NoSuchAlgorithmException) {
            UnsuportedAlgorithm(e, algorithm).failure()
        } catch (e: IOException) {
            SourceContentNotReadable(path, e).failure()
        }
    }
}

fun File.hashContentsV2(algorithm: String): Result<HashingError<Exception>, String> {

    val (digest, noSuchAlgorithm) = result<NoSuchAlgorithmException, MessageDigest> {
        MessageDigest.getInstance(
            algorithm
        ).success()
    }

    if (noSuchAlgorithm != null) {
        return UnsuportedAlgorithm(noSuchAlgorithm, algorithm).failure()
    }

    val (hash, fileReadException) = result<IOException, String> {
        digest.get().run {
            forEachBlock { buffer, bytesRead ->
                if (bytesRead > 0) {
                    update(buffer, 0, bytesRead)
                }
            }
            encodeHexString(digest()).success()
        }
    }

    if (fileReadException != null) {
        return SourceContentNotReadable(path, fileReadException).failure()
    }

    return hash.get().success()
}

fun File.hashContentsV3(algorithm: String): Result<HashingError<Exception>, String> {
    return result<Exception, MessageDigest> { MessageDigest.getInstance(algorithm).success() }.thenResult {
        forEachBlock { buffer, bytesRead ->
            if (bytesRead > 0) {
                value.update(buffer, 0, bytesRead)
            }
        }
        encodeHexString(value.digest()).success()
    }.mapFailure { e ->
        when (e) {
            is IOException -> SourceContentNotReadable(path, e)
            is NoSuchAlgorithmException -> UnsuportedAlgorithm(e, algorithm)
            else -> throw e
        }
    }
}


fun File.hashContentsV4(algorithm: String): Result<HashingError<Exception>, String> {
    return result<NoSuchAlgorithmException, MessageDigest> {
        MessageDigest.getInstance(algorithm).success()
    }.exceptOn {
        UnsuportedAlgorithm(error, algorithm).failure()
    }.thenResultCatching({ e: IOException -> SourceContentNotReadable(path, e) }) {
        forEachBlock { buffer, bytesRead -> if (bytesRead > 0) value.update(buffer, 0, bytesRead) }
        encodeHexString(value.digest()).success()
    }
}

fun File.hashContentsV5(algorithm: String): Result<HashingError<Exception>, String> {

    val unsupportedAlgorithm = fun(e: NoSuchAlgorithmException) = UnsuportedAlgorithm(e, algorithm)
    val inputFailure = fun(e: IOException) = SourceContentNotReadable(path, e)

    return resultCatching(unsupportedAlgorithm) {
        MessageDigest.getInstance(algorithm).success()
    }.thenResultCatching(inputFailure) {
        forEachBlock { buffer, bytesRead -> if (bytesRead > 0) value.update(buffer, 0, bytesRead) }
        encodeHexString(value.digest()).success()
    }
}