package com.acme.mediatranscoding

import assertk.Assert
import assertk.assertThat
import assertk.assertions.*
import com.acme.mediatranscoding.api.TranscodingError
import com.acme.mediatranscoding.support.I8n
import com.acme.mediatranscoding.support.I8n.MESSAGES_BUNDLE
import com.acme.mediatranscoding.support.humanizedName
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.*
import java.util.*
import java.util.ResourceBundle.getBundle
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.typeOf

@OptIn(ExperimentalStdlibApi::class)
internal class TranscodingErrorTest {

    //<editor-fold desc="Private fields">
    private val testTranscoder = "TestTranscoder"
    private val testMedia = "TestMedia"
    private val testInitErrorReported = "TestTranscoderInitializingError"
    private val availableTranscodingErrors =
        registerAvailableTranscoderErrors().also { registeredErrors ->
            assertThat(registeredErrors).isNotMissingActualImplementations()
        }
    //</editor-fold>

    //<editor-fold desc="Preparation">
    @BeforeAll
    fun prepareTests() {
        ResourceBundle.clearCache()
        assertThat { getBundle(MESSAGES_BUNDLE) }.isSuccess()
    }

    private fun registerAvailableTranscoderErrors(): List<TranscodingError> = listOf(
        TranscodingError.InvalidMediaFormat,
        TranscodingError.NothingToDo,
        TranscodingError.UnexpectedFailure(
            testTranscoder,
            Exception("Not expected"),
            "aDeveloperDebugNote"
        ),
        TranscodingError.InitFailed(testTranscoder, Exception(testInitErrorReported)),
        TranscodingError.MediaNotAvailable(testMedia, Exception("Test media not available")),
        TranscodingError.IllegalState(testTranscoder, "busy", "invoke()"),
        TranscodingError.UnsupportedTranscoding(testTranscoder, "pdf", "text/plain"),
    )

    //</editor-fold>

    //<editor-fold desc="Unit Tests">
    @ParameterizedTest
    @SourcedByTestableTranscodingErrors
    fun `All transcoding errors which has a 'cause' property should be propagated down the chain`(
        error: TranscodingError
    ) = assertThat(error).causeIsPropagatedIfRequested()

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
                Exception("UnexpectedErrorHere!"),
                "Not expected"
            )
        }.isSuccess().message().contains("UnexpectedErrorHere!")
    }

    @ParameterizedTest
    @SourcedByTestableTranscodingErrors
    fun `Developer error message should be usable if it is set transcoding error`(transcodingError: TranscodingError) {

        val developerErrorCode = transcodingError.developerErrorCode ?: return
        val developerErrorTemplate = I8n.message(developerErrorCode)

        assertThat(
            developerErrorTemplate,
            "Developer error message resource [%s] must exists transcoding error's [%s]".format(
                developerErrorCode,
                transcodingError.name
            )
        ).isInstanceOf(Result.Success::class).prop("result") {
            it.result as String?
        }.given { messageTemplate ->
            println("$developerErrorCode -> $messageTemplate")
            assertThat(
                messageTemplate,
                "Expected developer template $developerErrorTemplate to not be null or empty."
            ).isNotNull().isNotEmpty()
        }

        assertThat {
            transcodingError.developerMessage()?.get()
        }.isSuccess().isNotNull().isNotEmpty()

    }
    //</editor-fold>

    //<editor-fold desc="Test Drivers">
    @Suppress("unused")
    fun availableErrorsRegistrations() = availableTranscodingErrors

    @MethodSource("availableErrorsRegistrations")
    annotation class SourcedByTestableTranscodingErrors
    //</editor-fold>

    //<editor-fold desc="Assertions">

    private fun Assert<TranscodingError>.message() =
        transform("ConversionError.message", TranscodingError::message)

    private fun Assert<TranscodingError>.causeIsPropagatedIfRequested() {

        given { actual: TranscodingError ->
            val actualErrorType = actual.javaClass.kotlin

            val causeProperty = actualErrorType.memberProperties.firstOrNull { property ->
                property.name == "cause"
                        && property.returnType.isSubtypeOf(typeOf<Throwable>())
            } ?: return

            println("Cause propagation requested: ${actual.name}")
            val cause = causeProperty.get(actual) as Throwable?
            assertThat { actual.throwing() }.isSuccess().prop(Exception::cause).isEqualTo(cause)
        }
    }

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
    //</editor-fold>

}
