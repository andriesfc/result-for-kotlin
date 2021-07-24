package com.acme.mediatranscoding.transcoders.poc

import assertk.assertThat
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class PageSizeTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "-1,-1",
            "1,-1",
            "-1,1"
        ]
    )
    fun customPageSizeDoesAcceptInvalidDimensions(width: Int, height: Int) {
        assertThat { PageSize.Custom(width, height) }
            .isFailure()
            .isInstanceOf(IllegalArgumentException::class).given { println(it.message) }
    }
}