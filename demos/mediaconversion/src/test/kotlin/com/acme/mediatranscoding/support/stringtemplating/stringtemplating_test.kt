package com.acme.mediatranscoding.support.stringtemplating

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import org.junit.jupiter.api.Test
import java.io.IOException

internal class StringTemplatingTest {


    @Test
    fun testBasicUsage() {
        val model = object  {
            val transcoder = "TestConverter"
            val cause = IOException("Boom!")
        }
        val expected = "Converter (${model.transcoder}) caused error: [${model.cause.javaClass.name}]: ${model.cause.message}"
        val template = "Converter ({{transcoder}}) caused error: [{{cause.class.name}}]: {{cause.message}}"
        assertThat { template.eval(model).get().also(::println) }.isSuccess().isEqualTo(expected)
    }

}