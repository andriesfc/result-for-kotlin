package com.acme.mediaconversion.provider.poc

import com.acme.mediaconversion.ConversionError
import com.acme.mediaconversion.ConversionError.ConverterInitFailed
import com.acme.mediaconversion.MediaConversionProvider
import com.acme.mediaconversion.MediaConversionProvider.Converter
import resultk.*
import java.io.*
import java.nio.charset.Charset

object PocMediaConversionProvider : MediaConversionProvider {

    override val providerId: String = "poc_media_converter"
    override val supportedMedias: Set<String> get() = MediaType.types

    override fun getConverter(
        mediaIn: String,
        mediaOut: String
    ): Result<ConversionError, Converter> = resultOf {
        when {
            mediaIn == MediaType.PDF && mediaOut == MediaType.PLAIN_TEXT -> PdfToTextConverter().success()
            else -> ConversionError.UnsupportedConversion(this, mediaIn, mediaOut).failure()
        }
    }

    override fun toString(): String = providerId

    class PdfToTextConverter internal constructor() : Converter {

        private enum class State {
            NOT_READY,
            IN_USE,
            READY
        }

        enum class HeaderSelection {
            FIRST_NON_EMPTY_LINE
        }

        private var headerSelection: HeaderSelection
        private var state = State.NOT_READY
        private val selfReady = Result.Success(this)
        private lateinit var charset: Result<Exception, Charset>

        init {
            setCharset("utf8")
            headerSelection = HeaderSelection.FIRST_NON_EMPTY_LINE
        }

        fun setCharset(charset: String) {
            this.charset = resultOf { Charsets.UTF_8.success() }
        }

        fun setHeaderSelection(headerSelection: HeaderSelection) {
            this.headerSelection = headerSelection
        }

        override fun init(): Result<ConversionError, Converter> = resultOf {
            when (state) {
                State.NOT_READY -> {
                    resultOfCatching({ ex: Exception -> ConverterInitFailed(this, ex) }) {
                        doInit()
                        state = State.READY
                        success()
                    }
                }
                State.IN_USE -> ConversionError.IllegalState(
                    this,
                    state.name.lowercase(),
                    "init"
                ).failure()
                State.READY -> selfReady
            }
        }

        private fun doInit() {
            charset.get() // Just raise here, its gets handled upstream
        }

        override fun invoke(
            sourceId: String,
            source: InputStream,
            destinationId: String,
            destination: OutputStream
        ): Result<ConversionError, Long> {

            var iostate = IOState.Waiting
            val exceptionHandler = fun(ex: Exception): ConversionError = when (ex) {
                is IllegalStateException -> ConversionError.IllegalState(
                    this,
                    state.name.lowercase(),
                    "invoke()"
                )
                is IOException -> when (iostate) {
                    IOState.Reading -> ConversionError.MediaNotAvailable(sourceId, ex)
                    IOState.Writing -> ConversionError.MediaNotAvailable(destinationId, ex)
                    IOState.Waiting -> ConversionError.UnexpectedFailure(this, ex)
                }
                else -> ConversionError.UnexpectedFailure(this, ex)
            }

            return resultOfCatching(exceptionHandler) {
                when (val bytesProcessed =
                    textToPdf(
                        statefulReader(source) { iostate = it },
                        statefulOutput(destination) { iostate = it }
                    )) {
                    0L -> ConversionError.NothingToDo.failure<ConversionError, Long>()
                    else -> bytesProcessed.success()
                }
            }
        }

        private fun textToPdf(reader: BufferedReader, outputStream: BufferedOutputStream): Long {
            TODO()
        }

        private fun statefulReader(
            source: InputStream,
            setState: (IOState) -> Unit
        ): BufferedReader {
            return object : InputStream() {
                override fun read(): Int {
                    setState(IOState.Reading)
                    return try {
                        source.read()
                    } finally {
                        setState(IOState.Waiting)
                    }
                }
            }.bufferedReader(charset.get())
        }

        private fun statefulOutput(
            outputStream: OutputStream,
            setState: (IOState) -> Unit
        ): BufferedOutputStream {
            return object : OutputStream() {
                override fun write(b: Int) {
                    setState(IOState.Writing)
                    try {
                        outputStream.write(b)
                    } finally {
                        setState(IOState.Waiting)
                    }
                }
            }.buffered()
        }
    }

    private enum class IOState {
        Reading,
        Writing,
        Waiting
    }

    object MediaType {
        const val PDF = "application/pdf"
        const val PLAIN_TEXT = "text/plain"
        val types = setOf(PDF, PLAIN_TEXT)
    }
}