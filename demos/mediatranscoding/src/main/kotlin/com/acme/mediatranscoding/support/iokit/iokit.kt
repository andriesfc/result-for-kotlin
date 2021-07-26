package com.acme.mediatranscoding.support.iokit

import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStream

fun InputStream.byteCountingInputStream(updateByteCount: (Int) -> Unit): InputStream {
    return object : InputStream() {
        private val updateCount = fun(bytesOut: Int) {
            if (bytesOut > 0) {
                updateByteCount(bytesOut)
            }
        }
        private val source = this@byteCountingInputStream
        override fun read(): Int = source.read().also(updateCount)
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            return source.read(b, off, len).also(updateCount)
        }

        override fun readAllBytes(): ByteArray {
            return source.readAllBytes().also { updateCount(it.size) }
        }

        override fun readNBytes(len: Int): ByteArray {
            return source.readNBytes(len).also { updateCount(it.size) }
        }
    }
}

interface ClosableSequence<T> : Sequence<T>, Closeable

fun BufferedReader.closableLineSequence(): ClosableSequence<String> {
    return lineSequence().let { sequence ->
        object : Sequence<String> by sequence, ClosableSequence<String> {
            private var closed = false
            override fun close() {
                if (!closed) {
                    this@closableLineSequence.close()
                    closed = true
                }
            }
        }
    }
}
