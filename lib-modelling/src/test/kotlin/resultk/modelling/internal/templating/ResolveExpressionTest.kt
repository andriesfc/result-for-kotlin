@file:Suppress("MemberVisibilityCanBePrivate")

package resultk.modelling.internal.templating

import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import resultk.Result
import resultk.map
import resultk.modelling.internal.templating.ExpressionResolver.PostProcessor.UnhandledExpressionProcessor
import resultk.modelling.internal.templating.ExpressionResolver.PostProcessor.UnhandledExpressionProcessor.*
import resultk.modelling.internal.templating.fixture.testcasemodel.TestCaseModel
import resultk.modelling.internal.templating.fixture.testcasemodel.mapped
import resultk.modelling.internal.templating.fixture.testcasemodel.mappedByJavaProps

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolveExpressionTest {

    private val defaultTemplate = "{{today}}, is a {{kind}} test for number {{n}}."
    private val defaultExpectedMessage: String // = "2021-03-12, is a simple test for number 12."
    private val defaultTestCase: TestCaseModel

    init {
        verifyAllResolversAreTestable()
        defaultTestCase = TestCaseModel.default
        defaultExpectedMessage = with(defaultTestCase) {
            "$today, is a $kind test for number $n."
        }
    }

    @MethodSource("allTestableResolvers")
    @ParameterizedTest
    fun `Test resolver resolves all`(model: TestCaseModel, resolver: ExpressionResolver) {

        val unresolvedKeys = model
            .mapped().keys
            .filterNot { expr -> resolver.accepts(expr).also { resolver.eval(expr) } }

        assertThat(unresolvedKeys, "unresolvedKeys").isEmpty()
    }

    @MethodSource("allTestableResolvers")
    @ParameterizedTest
    fun `Test eval of default with`(model: TestCaseModel, resolver: ExpressionResolver) {
        assertThat { defaultTemplate.eval(resolver).map(StringBuilder::toString).get() }
            .isSuccess()
            .isEqualTo(defaultExpectedMessage)
    }

    @Test
    fun `Evaluating ignored expressions preserves the template as is`() {
        val resolver = mockk<ExpressionResolver>(moreInterfaces = arrayOf(
            UnhandledExpressionProcessor::class)) {
            this as UnhandledExpressionProcessor
            every { accepts(any()) } returns false
            every { postProcess(any()) } returns UnprocessedExpressionResolution.Ignore
        }

        val template = "{{ one }} {{ two }} {{ three }} {{ four }}"
        val actual = template.eval(resolver)
        println(actual)

        assertThat(actual).all {
            isInstanceOf(Result.Success::class)
            transform("actual.result") { it.get().toString() }.isEqualTo(template)
        }
    }

    fun allTestableResolvers() =
        TestCaseModel.default.testableResolvers().map { (model, resolver) ->
            Arguments.of(model, resolver)
        }

    private fun verifyAllResolversAreTestable() {

        val presentInCodebase =
            ResolveExpression::class.sealedSubclasses.map { it.javaObjectType }.toSet()

        val availableToTest = allTestableResolvers().map(Arguments::get).map { args -> args[1] }

        val missing = presentInCodebase.filter { expectedClz ->
            null == availableToTest.find { actual ->
                expectedClz.isInstance(actual)
            }
        }

        if (missing.isEmpty()) {
            return
        }

        throw Exception(buildString {
            appendLine("Please add the following resolvers to the Model.testableResolvers() extension function: ")
            missing.forEachIndexed { index, clazz ->
                appendLine("(${index + 1}) : ${clazz.name.replace('$', '.')}")
            }
        })
    }

    private fun TestCaseModel.testableResolvers() = listOf(
        this to ResolveExpression.ByMapLookup(mapped()),
        this to ResolveExpression.ByPropertiesLookup(mappedByJavaProps()),
        this to ResolveExpression.ByBeanModel(this),
        this to mapped().run { ResolveExpression.ByLookupFunction(this::get, this::containsKey) }
    )
}