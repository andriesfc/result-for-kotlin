package com.acme.mediatranscoding.api

import com.acme.mediatranscoding.support.I8n
import com.acme.mediatranscoding.support.humanizedName
import org.slf4j.LoggerFactory
import resultk.ThrowableProvider
import resultk.getOrNull

//region Transcoding API

class TranscodingException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

sealed class TranscodingError(
    val errorCode: String,
    val developerErrorCode: String? = null
) : ThrowableProvider<TranscodingException> {

    val name: String get() = "${javaClass.humanizedName}[$errorCode]"

    object InvalidMediaFormat :
        TranscodingError("error.transcoding.invalid_input_format")

    object NothingToDo :
        TranscodingError("error.transcoding.nothing_to_do")

    class UnexpectedFailure<T>(
        val transcoder: T,
        val cause: Exception,
        val developerDebugNote: String
    ) :
        TranscodingError(
            errorCode = "error.transcoding.unexpected_failure",
            developerErrorCode = "error.transcoding.unexpected_failure.developer"
        ) {
        override fun message(): String = super.message().format(cause.message)
        override fun throwing(message: String): TranscodingException =
            TranscodingException(message, cause)
    }

    class InitFailed<out T : Any>(val transcoder: T, val cause: Exception) :
        TranscodingError("error.transcoding.init_failed") {
        override fun message(): String = super.message().format(transcoder, cause.message)
        override fun throwing(message: String): TranscodingException =
            TranscodingException(message, cause)
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

    class MediaNotAvailable<out T : Any>(val media: T, val cause: Exception?) : TranscodingError(
        "error.transcoding.media_not_available",
        "error.transcoding.media_not_available.developer"
    ) {
        override fun message(): String = super.message().format(media)
        override fun throwing(message: String): TranscodingException =
            TranscodingException(message, cause)
    }

    // ------- Behaviour shared by all error codes (some are modifiable) ---
    private fun template() = I8n.message(errorCode)
    override fun toString(): String = developerMessage()?.get() ?: message()
    open fun message(): String = template().get()
    protected open fun developerMessageModel(): Any = this
    private fun throwableMessage(): String = developerMessage()?.getOrNull() ?: message()
    fun developerMessage() = developerErrorCode?.let { I8n.eval(it, developerMessageModel()) }
    override fun throwing(): TranscodingException = throwing(throwableMessage())
    protected open fun throwing(message: String): TranscodingException {
        return TranscodingException(message)
    }

    companion object {
        private val log = LoggerFactory.getLogger(TranscodingError::class.java)
    }
}

//endregion
