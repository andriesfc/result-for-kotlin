package com.acme.mediatranscoding.api

import com.acme.mediatranscoding.support.I8n
import org.slf4j.LoggerFactory
import resultk.Result
import java.io.InputStream
import java.io.OutputStream

//region Transcoding API

sealed class TranscodingError(
    val errorCode: String,
    val developerErrorCode: String? = null
) {

    object InvalidMediaFormat :
        TranscodingError("error.transcoding.invalid_input_format")

    object NothingToDo :
        TranscodingError("error.transcoding.nothing_to_do")

    class UnexpectedFailure<T>(val transcoder: T, val cause: Exception) :
        TranscodingError(
            errorCode = "error.transcoding.unexpected_failure",
            developerErrorCode = "error.transcoding.unexpected_failure.developer"
        ) {
        override fun message(): String = super.message().format(cause.message)
    }

    class InitFailed<out T : Any>(val transcoder: T, val cause: Exception) :
        TranscodingError("error.transcoding.init_failed") {
        override fun message(): String {
            return super.message().format(transcoder, cause.message)
        }
    }

    class IllegalState<T>(
        val transcoder: T,
        val currentState: String,
        val operationRequested: String
    ) : TranscodingError(
        "error.transcoding.illegal_state_during_operation",
        "error.transcoding.illegal_state_during_operation.developer"
    )

    class UnsupportedTranscoding<T>(
        val provider: T,
        val inputMediaTypeRequest: String,
        val outputMediaTypeRequested: String
    ) : TranscodingError("error.transcoding.conversion_not_supported")

    class MediaNotAvailable<out T : Any>(val media: T, val cause: Exception?) :
        TranscodingError("error.transcoding.media_not_available")

    private fun template() = I8n.message(errorCode)

    override fun toString(): String {
        return developerMessage()?.get() ?: message()
    }

    open fun message(): String = template().get()

    protected open fun developerMessageModel(): Any = this

    fun developerMessage() = developerErrorCode?.let {
        I8n.eval(it, developerMessageModel())
    }

    companion object {
        private val log = LoggerFactory.getLogger(TranscodingError::class.java)
    }
}


interface Transcoder {

    enum class State {
        READY /* Ready to transcode */,
        BUSY /* Busy transcoding now */,
        DISPOSED /* Transcoder has been disposed! */,
    }

    val providerId: String
    val mediaIn: String
    val mediaOut: String

    fun state(): State

    fun dispose()

    operator fun invoke(
        sourceId: String,
        source: InputStream,
        destinationId: String,
        destination: OutputStream,
    ): Result<TranscodingError, Long>
}

//endregion

//region Supporting & Base classes

//endregion
