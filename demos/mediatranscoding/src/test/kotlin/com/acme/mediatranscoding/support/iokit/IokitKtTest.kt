package com.acme.mediatranscoding.support.iokit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.InputStream
import kotlin.random.Random

class IokitKtTest {

    @Nested
    inner class TestByteCountingInputStream {

        private var bytesCounted: Int = -1
        private lateinit var givenData: ByteArray
        private lateinit var dataStream: InputStream

        @BeforeEach
        fun setUp() {
            bytesCounted = 0
            val expectedByteCount = Random.nextInt(100, 600)
            givenData = Random.nextBytes(expectedByteCount)
            dataStream = givenData
                .inputStream()
                .byteCountingInputStream { bytes -> bytesCounted += bytes }
        }

        @Test
        fun `Test readFully should update counter correctly`() {
            assertThat { dataStream.readAllBytes() }.isSuccess()
            assertThat(bytesCounted).isEqualTo(givenData.size)
        }
    }

    @Nested
    inner class CloseableLineSequence {

        @Test
        fun `Closeable Sequence Should Close Underlying Reader`() {

            val text = """
            Line 1
            Line 2
            Line 3
            Line 4
             """.trimIndent()

            val reader = spyk(text.reader().buffered()) {
                every { close() } just Runs
            }

            val closableSequence = reader.closableLineSequence()

            closableSequence.close()
            closableSequence.close()

            verify(exactly = 1) { reader.close() }

        }

    }
}
