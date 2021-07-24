package com.acme.mediatranscoding.transcoders.poc

import com.acme.mediatranscoding.api.Transcoder
import com.acme.mediatranscoding.api.TranscodingError
import com.acme.mediatranscoding.api.TranscodingError.NothingToDo
import com.acme.mediatranscoding.support.Counter
import com.acme.mediatranscoding.support.I8n
import com.acme.mediatranscoding.support.humanizedName
import com.acme.mediatranscoding.support.iokit.byteCountingInputStream
import resultk.Result
import resultk.failure
import resultk.success
import java.io.InputStream
import java.io.OutputStream


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
    private val paragraphFlow: ParagraphFlow = ParagraphFlow.ENABLED
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
            TranscodingError.UnexpectedFailure(this, ex)
        }
        return resultk.resultOfCatching(unhandledException) {
            val bytesCounter = Counter()
            source.byteCountingInputStream { bytesCounter += it.toLong() }
            when (val processed = bytesCounter.get()) {
                0L -> NothingToDo.failure()
                else -> processed.success()
            }
        }
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
}


//endregion




