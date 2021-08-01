@file:Suppress("MemberVisibilityCanBePrivate")

package resultk.modelling.internal.templating

import assertk.all
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.Result
import resultk.map
import resultk.modelling.internal.InternalModellingError
import resultk.modelling.internal.templating.ExpressionResolver.PostProcessor.*
import resultk.modelling.internal.templating.ExpressionResolver.PostProcessor.UnhandledExpressionProcessor.UnprocessedExpressionResolution
import resultk.modelling.internal.templating.ExpressionResolver.PostProcessor.UnhandledExpressionProcessor.UnprocessedExpressionResolution.FailOnlyWithThese
import resultk.modelling.internal.templating.fixture.testcasemodel.TestCaseModel
import java.time.LocalDate
import java.time.Month
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
    fun `Verify correct behaviour post process unhandled expressions`(resolution: UnprocessedExpressionResolution) {

        resolver = mockk(moreInterfaces = arrayOf(UnhandledExpressionProcessor::class)) {
            this as UnhandledExpressionProcessor
            every { accepts(any()) } returns false
            every { postProcess(any()) } returns resolution
        }

        val result: Result<InternalModellingError, String> =
            template.eval(resolver).map(StringBuilder::toString)

        print("******( $resolution -> $result )******")

        when (resolution) {
            is FailOnlyWithThese -> assertAll {
                assertThat(result).prop(Result<*, *>::isFailure).isTrue()
                assertThat(resolution.failedExpressions).isEqualTo(failOnlyWithThese.failedExpressions)
            }
            UnprocessedExpressionResolution.Ignore -> assertThat(result).all {
                prop(Result<*, *>::isFailure).isFalse()
                transform { it.get() }.isEqualTo(template)
            }
            UnprocessedExpressionResolution.IsFailure -> assertAll {
                assertThat(result).prop(Result<*, *>::isFailure).isTrue()
            }
        }

        verify { (resolver as UnhandledExpressionProcessor).postProcess(any()) }
    }

    @Test
    fun `Verify ability to post process the final buffer`() {

        val model = TestCaseModel(
            kind = "hot",
            today = LocalDate.of(2021, Month.JANUARY, 1),
            n = 33
        )
        val template = "{{ today }} is kind of {{ kind }} at a temp of {{ n }}"
        val beforePostProcess = "${model.today} is kind of ${model.kind} at a temp of ${model.n}"
        val stripWhitespace = fun CharSequence.() = filterNot(Char::isWhitespace)
        val resolver = object : FinalBuffer,
            ExpressionResolver by ResolveExpression.ByBeanModel(model) {
            override fun postProcess(result: StringBuilder) {
                val stripped = result.stripWhitespace()
                result.clear()
                result.append(stripped)
            }
        }

        val actual = template.eval(resolver).map(StringBuilder::toString)
        val expected = beforePostProcess.stripWhitespace().toString()

        println(actual)

        assertThat(actual)
            .isInstanceOf(Result.Success::class)
            .transform { it.result }.isEqualTo(expected)
    }

    fun unprocessedExpressionResolutions(): List<UnprocessedExpressionResolution> = listOf(
        UnprocessedExpressionResolution.Ignore,
        UnprocessedExpressionResolution.IsFailure,
        failOnlyWithThese
    )

}