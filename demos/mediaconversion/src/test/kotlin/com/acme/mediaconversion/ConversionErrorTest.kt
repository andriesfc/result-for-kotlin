package com.acme.mediaconversion

import assertk.Assert
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.Result
import java.util.*
import java.util.ResourceBundle.getBundle

internal class ConversionErrorTest {

    private val testConverter = "TestConverter"
    private val testMedia = "TestMedia"
    private val testInitErrorReported = "TestInitError"
    private lateinit var availableErrorsRegistration: List<ConversionError>

    @BeforeAll
    fun setAllTests() {

        ResourceBundle.clearCache()

        assertThat { getBundle(ERROR_MESSAGES) }.isSuccess()

        registerAllAvailableInstances()
        availableErrorsRegistration =
            availableErrorsRegistrations().sortedBy(ConversionError::errorCode)

        // Determine which types are missing in the available types,
        // and fail all tests henceforth!
        val foundRegisteredInstanceOf = fun(type: Class<out ConversionError>): Boolean {
            return availableErrorsRegistration.count { type.isInstance(it) } == 1
        }

        val notFoundInAvailable = ConversionError::class.sealedSubclasses
            .map { it.javaObjectType }
            .filterNot(foundRegisteredInstanceOf)

        assertThat(
            notFoundInAvailable,
            "Please the following error instances to the `availableErrorsRegistration` list:"
        ).isEmpty()
    }

    private fun registerAllAvailableInstances() {
        availableErrorsRegistration = listOf(
            ConversionError.InvalidMediaFormat,
            ConversionError.NothingToDo,
            ConversionError.UnexpectedFailure(
                testConverter,
                Exception("Not expected")
            ),
            ConversionError.ConverterInitFailed(
                testConverter,
                Exception(testInitErrorReported)
            ),
            ConversionError.MediaNotAvailable(
                testMedia,
                Exception("Test media not available")
            )
        )
    }

    @ParameterizedTest
    @TestRegisteredErrorCodeSource
    fun conversionErrorIsMappedToResource(e: ConversionError) {
        println(e)
        assertThat {
            getBundle(ERROR_MESSAGES).getString(e.errorCode)
        }.isSuccess()
    }

    @ParameterizedTest
    @TestRegisteredErrorCodeSource
    fun conversionErrorHasProperMessage(e: ConversionError) {
        println(e.errorCode + "=" + e.message())
        assertThat(e).also {
            it.prop("message", ConversionError::message).isNotEmpty()
        }
    }

    @Test
    fun converterInitializingErrorShouldMentionTheConverterAndFailure() {
        assertThat(
            ConversionError.ConverterInitFailed(
                converter = testConverter,
                cause = Exception((testInitErrorReported))
            )
        ).message().contains(testConverter, testInitErrorReported)
    }

    @Test
    fun `Unexpected FailureError Should contain unexpected message`() {
        assertThat {
            ConversionError.UnexpectedFailure(
                "testConverter",
                Exception("UnexpectedErrorHere!")
            )
        }.isSuccess().message().contains("UnexpectedErrorHere!")
    }

    @ParameterizedTest
    @TestRegisteredErrorCodeSource
    fun `Developer error message should be mapped if set`(e: ConversionError) {
        val developerMessage = e.developerMessage() ?: return
        assertThat(developerMessage)
            .isInstanceOf(Result.Success::class)
            .transform("result.success.value", Result.Success<*>::value).isNotNull()
            .isInstanceOf(String::class)
            .isNotNull()
            .isNotEmpty()
    }

    @Suppress("unused")
    fun availableErrorsRegistrations() = availableErrorsRegistration

    @MethodSource("availableErrorsRegistrations")
    annotation class TestRegisteredErrorCodeSource

    private fun Assert<ConversionError>.message() =
        transform("ConversionError.message", ConversionError::message)
}
