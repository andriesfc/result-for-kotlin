package resultk.modelling.internal

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isSuccess
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.modelling.internal.InternalModellingError.*
import kotlin.random.Random
import kotlin.reflect.jvm.jvmName


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InternalModellingErrorTest {

    init {
        verifyTestDeclaredAllPossibleModelingErrors()
    }

    @ParameterizedTest
    @MethodSource("testableExamples")
    fun `All internal modelling errors should have error messages`(e: InternalModellingError) {
        assertThat { e.message() }
            .isSuccess()
            .apply { given(::println) }
            .isNotEmpty()
    }

    private fun testableExamples() = declaredTestExamples().toList().toTypedArray()

    private fun declaredTestExamples() = sequenceOf(
        UnresolvedTemplateExpression("Some template is {{stated}},", listOf("stated")),
        UnexpectedFailure(Exception("Not expected!")),
        MalformedTemplate(
            template = "someTemplate",
            index = "someTemplate".length.random(),
            reportedVia = this,
            cause = null
        )
    )

    private fun verifyTestDeclaredAllPossibleModelingErrors() {
        val availableInCodeBase = InternalModellingError::class.sealedSubclasses.toSet()
        val declaredInTest = declaredTestExamples().toSet()
        val undeclaredByTest = availableInCodeBase.filter { expectedErrorClass ->
            declaredInTest.indexOfFirst { exampleInTest ->
                expectedErrorClass.isInstance(
                    exampleInTest
                )
            } == NOT_FOUND
        }
        require(undeclaredByTest.isEmpty()) {
            buildString {
                appendLine()
                appendLine("****************************************************************************************************")
                appendLine("*   Please add the following example InternalModelError to the declaredTestExamples() sequence     *")
                appendLine("****************************************************************************************************")
                undeclaredByTest.forEachIndexed { index, kClass ->
                    appendLine(" (${index + 1}) : ${kClass.jvmName.replace('$', '.')}")
                }
                appendLine("****************************************************************************************************")
                appendLine()
            }
        }
    }

}

private fun Int.random(): Int = Random.nextInt(0, this)
private const val NOT_FOUND = -1