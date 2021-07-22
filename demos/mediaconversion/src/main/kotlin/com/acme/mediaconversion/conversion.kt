package com.acme.mediaconversion

import com.acme.mediaconversion.support.stringtemplating.eval
import org.slf4j.LoggerFactory
import resultk.*
import java.io.InputStream
import java.io.OutputStream
import java.util.*

sealed class ConversionError(
    val errorCode: String,
    private val developerErrorCode: String? = null
) {

    object InvalidMediaFormat :
        ConversionError("error.media_conversion.invalid_input_format")

    object NothingToDo :
        ConversionError("error.media_conversion.nothing_to_do")

    class UnexpectedFailure<T>(val converter: T, val cause: Exception) :
        ConversionError(
            errorCode = "error.media_conversion.unexpected_failure",
            developerErrorCode = "error.media_conversion.unexpected_failure.developer"
        ) {
        override fun message(): String = super.message().format(cause.message)
    }

    class ConverterInitFailed<T : Any>(val converter: T, val cause: Exception) :
        ConversionError("error.media_conversion.init_failed") {
        override fun message(): String {
            return super.message().format(converter, cause.message)
        }
    }

    class IllegalState<T>(
        val converter: T,
        val currentState: String,
        val operationRequested: String
    ) : ConversionError(
        "error.media_conversion.illegal_state_during_operation",
        "error.media_conversion.illegal_state_during_operation.developer"
    )

    class UnsupportedConversion<T>(
        val provider: T,
        val inputMediaTypeRequest: String,
        val outputMediaTypeRequested: String
    ) : ConversionError("error.media_conversion.conversion_not_supported")

    class MediaNotAvailable<out T : Any>(val media: T, val cause: Exception?) :
        ConversionError("error.media_conversion.media_not_available")

    private fun template() = resultOf<MissingResourceException, String> {
        ResourceBundle.getBundle(ERROR_MESSAGES).getString(errorCode).success()
    }.onFailure {
        log.error("Failed to load error template from {}: {}", it.key, ERROR_MESSAGES)
    }

    override fun toString(): String {
        return developerMessage()?.getOrNull() ?: message()
    }

    open fun message(): String = template().get()

    protected open fun developerMessageModel(): Any = this

    fun developerMessage() = developerErrorCode?.let {
        ResourceBundle
            .getBundle(ERROR_MESSAGES)
            .getString(developerErrorCode)
            .eval(developerMessageModel())
    }

    companion object {
        private val log = LoggerFactory.getLogger(ConversionError::class.java)
    }
}

internal const val ERROR_MESSAGES = "Messages"

interface MediaConversionProvider {

    val providerId: String
    val supportedMedias: Set<String>

    fun getConverter(
        mediaIn: String,
        mediaOut: String
    ): Result<ConversionError, Converter>

    interface Converter {
        fun init(): Result<ConversionError, Converter>
        operator fun invoke(
            sourceId: String,
            source: InputStream,
            destinationId: String,
            destination: OutputStream,
        ): Result<ConversionError, Long>
    }
}

