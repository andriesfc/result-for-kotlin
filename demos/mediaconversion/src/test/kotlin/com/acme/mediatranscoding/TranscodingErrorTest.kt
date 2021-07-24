package com.acme.mediatranscoding

import assertk.Assert
import assertk.assertThat
import assertk.assertions.*
import com.acme.mediatranscoding.api.TranscodingError
import com.acme.mediatranscoding.support.I8n.MESSAGES_BUNDLE
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.Result
import java.util.*
import java.util.ResourceBundle.getBundle

internal class TranscodingErrorTest {

    private val testTranscoder = "TestTranscoder"
    private val testMedia = "TestMedia"
    private val testInitErrorReported = "TestTranscoderInitializingError"
    private val availableTranscodingErrors =
        registerAvailableTranscoderErrors().also { registeredErrors ->
            assertThat(registeredErrors).allIsRegisteredForTesting()
        }

    /**
     * This assertion asserts that the list of testing errors supplied contains
     * covers all sub classes of the possible error types. This  is important to
     * ensure that all transcoding errors types are available for testing.
     */
    private fun Assert<List<TranscodingError>>.allIsRegisteredForTesting() {
        given { registrations ->

            val humanizedName = fun Class<*>.() = name.replace('$', '.')

            val allPossibleTranscodingErrorTypes =
                TranscodingError::class.sealedSubclasses.map { it.javaObjectType }

            val unregisteredErrorTypes = allPossibleTranscodingErrorTypes.filter { expected ->
                registrations.count { registration -> expected.isInstance(registration) } == 0
            }.map(humanizedName)

            if (unregisteredErrorTypes.isNotEmpty()) System.err.apply {
                println()
                println("*********************************************************************************************************")
                println("* Please add the following test transcoding errors via the registerAvailableTranscoderErrors() function *")
                println("*********************************************************************************************************")
                println()
                unregisteredErrorTypes.forEachIndexed { index, name ->
                    println("  ${index + 1}. $name")
                }
            }

            assertThat(unregisteredErrorTypes).isEmpty()
        }
    }

    @BeforeAll
    fun prepareTests() {
        ResourceBundle.clearCache()
        assertThat { getBundle(MESSAGES_BUNDLE) }.isSuccess()
    }

    private fun registerAvailableTranscoderErrors(): List<TranscodingError> = listOf(
        TranscodingError.InvalidMediaFormat,
        TranscodingError.NothingToDo,
        TranscodingError.UnexpectedFailure(testTranscoder, Exception("Not expected")),
        TranscodingError.InitFailed(testTranscoder, Exception(testInitErrorReported)),
        TranscodingError.MediaNotAvailable(testMedia, Exception("Test media not available")),
        TranscodingError.IllegalState(testTranscoder, "busy", "invoke()"),
        TranscodingError.UnsupportedTranscoding(testTranscoder, "pdf", "text/plain"),
    )

    @ParameterizedTest
    @TestRegisteredErrorCodeSource
    fun conversionErrorIsMappedToResource(e: TranscodingError) {
        assertThat { getBundle(MESSAGES_BUNDLE).getString(e.errorCode) }.isSuccess()
    }

    @ParameterizedTest
    @TestRegisteredErrorCodeSource
    fun conversionErrorHasProperMessage(e: TranscodingError) {
        println(e.errorCode + "=" + e.message())
        assertThat(e).also {
            it.prop("message", TranscodingError::message).isNotEmpty()
        }
    }

    @Test
    fun converterInitializingErrorShouldMentionTheConverterAndFailure() {
        assertThat(
            TranscodingError.InitFailed(
                testTranscoder,
                Exception((testInitErrorReported))
            )
        ).message().contains(testTranscoder, testInitErrorReported)
    }

    @Test
    fun `Unexpected FailureError Should contain unexpected message`() {
        assertThat {
            TranscodingError.UnexpectedFailure(
                "testConverter",
                Exception("UnexpectedErrorHere!")
            )
        }.isSuccess().message().contains("UnexpectedErrorHere!")
    }

    @ParameterizedTest
    @TestRegisteredErrorCodeSource
    fun `Developer error message should be exists if set`(e: TranscodingError) {
        val developerMessage = e.developerMessage() ?: return
        assertThat(
            developerMessage,
            "Message resource key [%s] to exists for transcoding error: [%s] ".format(
                e.developerErrorCode,
                e.javaClass.name.replace('$', '.')
            )
        ).isInstanceOf(Result.Success::class)
            .transform("result.success.value", Result.Success<*>::value)
            .isNotNull()
            .isInstanceOf(String::class)
            .isNotNull()
            .isNotEmpty()
    }

    @Suppress("unused")
    fun availableErrorsRegistrations() = availableTranscodingErrors

    @MethodSource("availableErrorsRegistrations")
    annotation class TestRegisteredErrorCodeSource

    private fun Assert<TranscodingError>.message() =
        transform("ConversionError.message", TranscodingError::message)

}
