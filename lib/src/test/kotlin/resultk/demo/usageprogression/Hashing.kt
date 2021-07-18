package resultk.demo.usageprogression

import org.apache.commons.codec.binary.Hex.encodeHexString
import resultk.*
import resultk.demo.domain.HashingError
import resultk.demo.domain.HashingError.SourceContentNotReadable
import resultk.demo.domain.HashingError.UnsupportedAlgorithm
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

fun File.hashContentsV0(algorithm: String): Result<HashingError<Exception>, String> {

    val messageDigest = try {
        MessageDigest.getInstance(algorithm)
    } catch (e: NoSuchAlgorithmException) {
        return UnsupportedAlgorithm(e, algorithm).failure()
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
    return resultOf {
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
            UnsupportedAlgorithm(e, algorithm).failure()
        } catch (e: IOException) {
            SourceContentNotReadable(path, e).failure()
        }
    }
}

fun File.hashContentsV2(algorithm: String): Result<HashingError<Exception>, String> {

    val (digest, noSuchAlgorithm) = resultOf<NoSuchAlgorithmException, MessageDigest> {
        MessageDigest.getInstance(
            algorithm
        ).success()
    }

    if (noSuchAlgorithm != null) {
        return UnsupportedAlgorithm(noSuchAlgorithm, algorithm).failure()
    }

    val (hash, fileReadException) = resultOf<IOException, String> {
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
    return resultOf<Exception, MessageDigest> { MessageDigest.getInstance(algorithm).success() }.thenResult {
        forEachBlock { buffer, bytesRead ->
            if (bytesRead > 0) {
                value.update(buffer, 0, bytesRead)
            }
        }
        encodeHexString(value.digest()).success()
    }.mapFailure { e ->
        when (e) {
            is IOException -> SourceContentNotReadable(path, e)
            is NoSuchAlgorithmException -> UnsupportedAlgorithm(e, algorithm)
            else -> throw e
        }
    }
}


fun File.hashContentsV4(algorithm: String): Result<HashingError<Exception>, String> {

    val unsupportedAlgorithm = fun(e: NoSuchAlgorithmException) = UnsupportedAlgorithm(e, algorithm)
    val inputFailure = fun(e: IOException) = SourceContentNotReadable(path, e)

    return resultCatching(unsupportedAlgorithm) {
        MessageDigest.getInstance(algorithm).success()
    }.thenResultCatching(inputFailure) {
        forEachBlock { buffer, bytesRead -> if (bytesRead > 0) value.update(buffer, 0, bytesRead) }
        encodeHexString(value.digest()).success()
    }
}