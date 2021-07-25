package com.acme.mediatranscoding.transcoders.poc

import com.acme.mediatranscoding.api.Transcoder
import com.acme.mediatranscoding.api.TranscodingError
import com.acme.mediatranscoding.api.TranscodingError.NothingToDo
import com.acme.mediatranscoding.api.TranscodingError.UnexpectedFailure
import com.acme.mediatranscoding.support.Counter
import com.acme.mediatranscoding.support.I8n
import com.acme.mediatranscoding.support.humanizedName
import com.acme.mediatranscoding.support.iokit.byteCountingInputStream
import resultk.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset


//region Pdf & Page modeling
object MediaTypes {
    const val PDF = "application/pdf"
    const val PLAIN_TEXT = "text/plain"
}

sealed class PageSize(val label: String, open val width: Int, open val height: Int) {

    object A4 : PageSize("A4", 210, 296)
    object A5 : PageSize("A5", 148, 210)

    class Custom(
        override val width: Int,
        override val height: Int
    ) : PageSize("Custom", width, height) {
        init {
            require(
                (height >= 0 && width >= 0),
                I8n.eval("com.acme.mediatranscoding.transcoders.poc.error.pageSize", this)::get
            )
        }
    }

    override fun toString(): String = "$label:${width}x${height}"
}

enum class Orientation {
    PORTRAIT,
    LANDSCAPE
}

enum class ParagraphFlow {
    ENABLED,
    KEEP_AS_IS
}
//endregion

//region Transcoder
class PdfTranscoder(
    private val pageSize: PageSize = PageSize.A4,
    private val pageOrientation: Orientation = Orientation.PORTRAIT,
    private val paragraphFlow: ParagraphFlow = ParagraphFlow.ENABLED,
    private val charset: Charset = Charsets.UTF_8
) : Transcoder {

    private var state = Transcoder.State.READY
    private var disposed = false
    override val providerId: String = javaClass.humanizedName
    override val mediaIn: String = MediaTypes.PLAIN_TEXT
    override val mediaOut: String = MediaTypes.PDF

    override fun state(): Transcoder.State = state

    override fun dispose() {
        this.disposed = true
    }

    override fun invoke(
        sourceId: String,
        source: InputStream,
        destinationId: String,
        destination: OutputStream
    ): Result<TranscodingError, Long> {
        val unhandledException = { ex: Exception ->
            UnexpectedFailure(
                transcoder = this,
                cause = ex,
                developerDebugNote = I8n.message("").get()
            )
        }
        return resultWithHandlingOf(unhandledException) {
            val bytesCounter = Counter()
            val textTokens = tokenizeText(
                sourceId,
                source.byteCountingInputStream {
                    bytesCounter += it.toLong()
                }.bufferedReader(charset)
            )

            when (val processed = bytesCounter.get()) {
                0L -> NothingToDo.failure()
                else -> processed.success()
            }
        }
    }

    private fun tokenizeText(
        sourceId: String,
        source: BufferedReader
    ): Sequence<Result<TranscodingError, SimpleTextParsing.TextNode>> {

        val errorReadingSourceMedia = { ex: IOException ->
            UnexpectedFailure(
                transcoder = this,
                cause = ex,
                developerDebugNote = I8n.message(
                    "com.acme.mediatranscoding.debugNote.sourceMediaNotReadable",
                    sourceId
                ).get()
            )
        }

        TODO()
    }
}


//endregion

//region Plain Text Document Parsing

internal object SimpleTextParsing {

    sealed class TextNode {
        data class Heading(val text: String) : TextNode()
        data class Paragraph(val text: String) : TextNode()
        object ParagraphBreak : TextNode()
        object EmptyLine : TextNode()
    }

    fun safeLineSequence(source: BufferedReader): Sequence<Result<IOException, String>> {
        return generateSequence {
            resultOf<IOException,String> { source.readLine().success() }
        }
    }

}


//endregion




