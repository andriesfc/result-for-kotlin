package com.acme.mediatranscoding.providers.poc

object PdfTranscoder {

    object MediaTypes {
        const val PDF = "application/pdf"
        const val PLAIN_TEXT = "text/plain"
    }

    sealed class PageSize(open val width: Int, open val height: Int) {

        object A4 : PageSize(210, 296)
        object A5 : PageSize(148, 210)

        class Custom(
            override val width: Int,
            override val height: Int
        ) : PageSize(width, height)

        companion object {
            fun fromString(s: String) : PageSize? {
                TODO()
            }
        }
    }

    enum class Orientation {
        PORTRAIT,
        LANDSCAPE
    }

    enum class ParagraphWrapping {
        ENABLED,
        KEEP_AS_IS
    }



}