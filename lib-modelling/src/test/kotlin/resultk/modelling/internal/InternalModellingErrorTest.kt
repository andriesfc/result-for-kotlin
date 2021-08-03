package resultk.modelling.internal

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isSameAs
import assertk.assertions.isSuccess
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import resultk.modelling.internal.InternalModellingError.MalformedTemplate
import resultk.modelling.internal.InternalModellingError.UnresolvedTemplateExpression
import kotlin.random.Random
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf


@ExperimentalStdlibApi
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

    @ParameterizedTest
    @MethodSource("testableDeclaringCause")
    fun `Raising internal modelling error should always throw the actual cause`(
        e: InternalModellingError,
        declaredCause: Throwable?
    ) {
        if (declaredCause != null) {
            assertThat(e.throwing()).isSameAs(declaredCause)
        }
    }

    private fun testableExamples() = declaredTestExamples().toList().toTypedArray()

    private fun testableDeclaringCause(): List<Arguments> {
        return declaredTestExamples().mapNotNull { e ->
            val p = e.javaClass.kotlin.memberProperties.firstNotNullOfOrNull { p ->
                when {
                    p.name != "cause" -> {
                        null
                    }
                    !(p.returnType.isSubtypeOf(typeOf<Throwable>())
                            || p.returnType.isSubtypeOf(
                        typeOf<Throwable?>()
                    )) -> {
                        null
                    }
                    else -> {
                        p
                    }
                }
            }
            p?.let { arguments(e, p.get(e)) }
        }.toList()
    }


    private fun declaredTestExamples() = sequenceOf(
        UnresolvedTemplateExpression("Some template is {{stated}},", listOf("stated")),
        MalformedTemplate(10.random(), "jkn asjhns {{ sksk  osls asllas lasl as", this, null),
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