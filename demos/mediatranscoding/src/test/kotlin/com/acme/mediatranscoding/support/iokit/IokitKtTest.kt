package com.acme.mediatranscoding.support.iokit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import kotlin.random.Random


class TestByteCountingInputStream {

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