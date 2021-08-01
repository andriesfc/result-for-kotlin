@file:Suppress("MemberVisibilityCanBePrivate")

package resultk.modelling.internal.templating

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isSuccess
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import resultk.map
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
    fun testResolverResolvesAll(model: TestCaseModel, resolver: ExpressionResolver) {

        val unresolvedKeys = model
            .mapped().keys
            .filterNot { expr -> resolver.accept(expr).also { resolver.eval(expr) } }

        assertThat(unresolvedKeys, "unresolvedKeys").isEmpty()
    }

    @MethodSource("allTestableResolvers")
    @ParameterizedTest
    fun testEvalOfDefaultWith(model: TestCaseModel, resolver: ExpressionResolver) {
        assertThat { defaultTemplate.eval(resolver).map(StringBuilder::toString).get() }
            .isSuccess()
            .isEqualTo(defaultExpectedMessage)
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