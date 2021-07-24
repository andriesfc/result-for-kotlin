package com.acme.mediatranscoding

import assertk.Assert
import assertk.assertThat
import assertk.assertions.*
import com.acme.mediatranscoding.api.TranscodingError
import com.acme.mediatranscoding.support.I8n.MESSAGES_BUNDLE
import com.acme.mediatranscoding.support.humanizedName
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.Result
import java.util.*
import java.util.ResourceBundle.getBundle
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
internal class TranscodingErrorTest {

    private val testTranscoder = "TestTranscoder"
    private val testMedia = "TestMedia"
    private val testInitErrorReported = "TestTranscoderInitializingError"
    private val availableTranscodingErrors =
        registerAvailableTranscoderErrors().also { registeredErrors ->
            assertThat(registeredErrors).isNotMissingActualImplementations()
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
    @SourcedByTestableTranscodingErrors
    fun `All transcoding errors which has 'cause' property should be propagted down the chain`(error: TranscodingError) {

        val errorType = error.javaClass::kotlin.get()

        val causeProperty = errorType.memberProperties.firstOrNull { p ->
            (p.name == "cause")
                    && p.returnType.isSubtypeOf(typeOf<Exception>())
        } ?: return

        val cause = causeProperty.get(error)
        val actual = error.throwing().cause

        assertThat(actual).isEqualTo(cause)
    }

    @ParameterizedTest
    @SourcedByTestableTranscodingErrors
    fun `Transcoding errorCode should be present in the message bundle`(e: TranscodingError) {
        assertThat { getBundle(MESSAGES_BUNDLE).getString(e.errorCode) }.isSuccess()
    }

    @ParameterizedTest
    @SourcedByTestableTranscodingErrors
    fun `Conversion Error Has Proper Message`(e: TranscodingError) {
        println(e.errorCode + "=" + e.message())
        assertThat(e).also {
            it.prop("message", TranscodingError::message).isNotEmpty()
        }
    }

    @Test
    fun `Converter Initializing Error Should Mention The Converter And Failure`() {
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
    @SourcedByTestableTranscodingErrors
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
    annotation class SourcedByTestableTranscodingErrors

    private fun Assert<TranscodingError>.message() =
        transform("ConversionError.message", TranscodingError::message)

    /**
     * This assertion ensures that all parameterized test annotated by the
     * [SourcedByTestableTranscodingErrors] receives all possible transcoding errors (NOTE:
     * it is up to the developer to keep the actual error instances by
     * updating the list returned via the [registerAvailableTranscoderErrors]
     * function up to date).
     */
    private fun Assert<List<TranscodingError>>.isNotMissingActualImplementations() {
        given { registrations ->

            val allPossibleTranscodingErrorTypes =
                TranscodingError::class.sealedSubclasses.map { it.javaObjectType }

            val unregisteredErrorTypes = allPossibleTranscodingErrorTypes.filter { expected ->
                registrations.count { registration -> expected.isInstance(registration) } == 0
            }.map(Class<*>::humanizedName)

            if (unregisteredErrorTypes.isNotEmpty()) System.err.apply {
                println()
                println("*********************************************************************************************************")
                println("* Please add the following test transcoding errors via the registerAvailableTranscoderErrors() function *")
                println("*********************************************************************************************************")
                println()
                unregisteredErrorTypes.forEachIndexed { index, typeName ->
                    println("  ${index + 1}. $typeName")
                }
            }

            assertThat(unregisteredErrorTypes).isEmpty()
        }
    }

}
