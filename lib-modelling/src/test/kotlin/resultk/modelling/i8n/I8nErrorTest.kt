package resultk.modelling.i8n

import assertk.assertThat
import assertk.assertions.isSuccess
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.modelling.testing.assertions.isNotEmptyOrBlank
import resultk.modelling.testing.assertions.peek
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

private const val NOT_FOUND = -1

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class I8nErrorTest {

    private val exampleResolverError: Throwable? = null
    private val exampleResolverErrorMessage: String = "Something happend while resolving template"
    private val exampleMalformedTemplate: String = "Some {{template}}"
    private val exampleBaseMessages = "MyMessages"
    private val exampleBaseLocale = Locale.getDefault()
    private val exampleMessageKey = "myMessageKeySample"

    init {
        verifyAllErrorsAreAvailableForTesting()
    }


    @ParameterizedTest
    @MethodSource("availableForTesting")
    fun `All i8n errors should produce valid message`(error: I8nError) {
        assertThat(error::message).isSuccess().peek(::println).isNotEmptyOrBlank()
    }


    private fun verifyAllErrorsAreAvailableForTesting() {
        val actualTypesAvailable = I8nError::class.sealedSubclasses
        val availableForTesting = availableForTesting()
        val missingFromThisTest = actualTypesAvailable
            .fold(mutableSetOf<KClass<out I8nError>>()) { acc, errClass ->
                if (availableForTesting.indexOfFirst(errClass::isInstance) == NOT_FOUND) {
                    acc += errClass
                }
                acc
            }
        require(missingFromThisTest.isEmpty()) {
            buildString {
                appendLine()
                appendLine("***********************************************************************************************")
                appendLine("*  Kindly add the following I8nError instances  to the availableForTesting() function below!  *")
                appendLine("***********************************************************************************************")
                missingFromThisTest.forEachIndexed { index, type ->
                    appendLine("(${index + 1}) : ${type.jvmName.replace('$','.')}")
                }
                appendLine()
            }
        }
    }

    private fun availableForTesting() = listOf(
        I8nError.MissingResourceBundle(exampleBaseMessages, exampleBaseLocale),
        I8nError.MissingMessageKey(exampleBaseMessages, exampleMessageKey, exampleBaseLocale),
        I8nError.MessageBuildFailure(
            baseName = exampleBaseMessages,
            templateMessageKey = exampleMessageKey,
            locale = exampleBaseLocale,
            template = exampleMalformedTemplate,
            resolverErrorMessage = exampleResolverErrorMessage,
            cause = exampleResolverError
        )
    )

}

