package com.acme.mediaconversion.support.stringtemplating

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import com.acme.mediaconversion.ConversionError
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class StringTemplatingTest {

    @Test
    fun testBasicUsage() {
        val model = ConversionError.ConverterInitFailed("testConverter", Exception("Boo!"))
        val expected = "Converter (${model.converter}) caused error: [${model.cause.javaClass.name}]: ${model.cause.message}"
        val template = "Converter ({{converter}}) caused error: [{{cause.class.name}}]: {{cause.message}}"
        assertThat { template.eval(model).get().also(::println) }.isSuccess().isEqualTo(expected)
    }

}