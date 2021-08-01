@file:Suppress("MemberVisibilityCanBePrivate")

package resultk.modelling.internal.templating

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.prop
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.Result
import resultk.map
import resultk.modelling.internal.InternalModellingError
import resultk.modelling.internal.templating.ExpressionResolver.PostProcessor.UnhandledExpressionProcessor
import resultk.modelling.internal.templating.ExpressionResolver.PostProcessor.UnhandledExpressionProcessor.UnprocessedExpressionResolution
import resultk.modelling.internal.templating.ExpressionResolver.PostProcessor.UnhandledExpressionProcessor.UnprocessedExpressionResolution.FailOnlyWithThese
import kotlin.reflect.jvm.jvmName
import kotlin.test.Test


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolverPostProcessingTest {

    private lateinit var resolver: ExpressionResolver
    private val expressions = listOf("one", "two", "three", "four")
    private val failOnlyWithThese = FailOnlyWithThese(expressions.shuffled().take(2))

    private val template =
        "{{${expressions[0]}}}" +
                " {{${expressions[1]}}}" +
                " {{${expressions[2]}}}" +
                " {{${expressions[3]}}}"

    init {

        val registrationHandler = this::unprocessedExpressionResolutions

        val missing = registrationHandler().let { configuredForTesting ->
            UnprocessedExpressionResolution::class.sealedSubclasses.filter { required ->
                configuredForTesting.indexOfFirst { required.isInstance(it) } == -1
            }
        }

        require(missing.isEmpty()) {
            buildString {
                appendLine("Please ensure the following post resolutions are returned by this function: ${registrationHandler.name}()")
                appendLine("------------------------------------------------------------------------------------------------------------------")
                missing.forEachIndexed { index, kClass ->
                    appendLine(" (${index + 1}) : ${kClass.jvmName.replace('$', '.')}")
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("unprocessedExpressionResolutions")
    fun testPostProcessUnhandledExpressions(resolution: UnprocessedExpressionResolution) {

        resolver = mockk(moreInterfaces = arrayOf(UnhandledExpressionProcessor::class)) {
            this as UnhandledExpressionProcessor
            every { accept(any()) } returns false
            every { postProcess(any()) } returns resolution
        }

        val result: Result<InternalModellingError, String> =
            template.eval(resolver).map(StringBuilder::toString)

        when (resolution) {
            is FailOnlyWithThese -> assertAll {
                assertThat(result).prop(Result<*, *>::isFailure).isTrue()
                assertThat(resolution.failedExpressions).isEqualTo(failOnlyWithThese.failedExpressions)
            }
            UnprocessedExpressionResolution.Ignore -> assertAll {
                assertThat(result.get()).isEqualTo(template)
                assertThat(result).transform { it.isSuccess }.isTrue()
            }
            UnprocessedExpressionResolution.IsFailure -> assertAll {
                assertThat(result).prop(Result<*, *>::isFailure).isTrue()
            }
        }

        verify { (resolver as UnhandledExpressionProcessor).postProcess(any()) }
    }

    @Test
    fun testPostProcessingFinalBuffer() {
    }

    fun unprocessedExpressionResolutions(): List<UnprocessedExpressionResolution> = listOf(
        UnprocessedExpressionResolution.Ignore,
        UnprocessedExpressionResolution.IsFailure,
        failOnlyWithThese
    )

}