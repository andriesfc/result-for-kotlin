package resultk.modelling.internal.templating

import assertk.Assert
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import io.mockk.every
import io.mockk.mockk
import resultk.Result
import resultk.component1
import resultk.component2
import resultk.errorOrNull
import java.time.LocalDate
import java.time.Month
import java.util.*
import kotlin.test.Test

internal class TemplatingKtTest {

    private val template = "{{today}}, is a {{kind}} test for number {{n}}."
    private val expected = "2021-03-12, is a simple test for number 12."
    private val bean = object {
        val n = 12
        val today = LocalDate.of(2021, Month.MARCH, 12)
        val kind = "simple"

        fun properties() = Properties().apply {
            setProperty("n", n.toString())
            setProperty("today", today.toString())
            setProperty("kind", kind)
        }

        fun map() = mapOf(
            "n" to n,
            "today" to today,
            "kind" to kind
        )
    }

    @Test
    fun `eval() should correctly identify all expressions and produce the correct result`() {
        val resolvable = bean.map()
        val resolver = mockk<ExpressionResolver>() {
            every { accept(any()) } answers { arg(0) in resolvable }
            every { eval(any()) } answers { resolvable[arg(0)] }
        }
        assertEvalOf(resolver).result().isEqualTo(expected)
    }

    @Test
    fun `Test standard resolvers`() {

        val resolvers = listOf(
            ResolveExpression.ByPropertiesLookup(bean.properties()),
            ResolveExpression.ByMapLookup(bean.map()),
            ResolveExpression.ByBeanModel(bean)
        )

        assertAll {
            resolvers.forEach { resolver ->
                assertEvalOf(resolver).result().isEqualTo(expected)
            }
        }
    }

    @Test
    fun testEvalViaBean() {

        val model = TemplatingError.MissingMessagePlaceholders(
            template = "Test template {{p1}}, {{p2}}, {{p3}}",
            placeholders = listOf("p1", "p2", "p3"))

        val template = "[{{errorKey}}] missing: {{placeholders}} (see <{{template}}>)"
        val (message, error) = template.eval(ResolveExpression.ByBeanModel(model))

        assertThat(error).isNull()
        println(message.get())
    }

    private fun assertEvalOf(resolver: ExpressionResolver) =
        assertThat { template.eval(resolver) }.isSuccess()

    private fun Assert<Result<TemplatingError, StringBuilder>>.error() =
        transform { it.errorOrNull() }.isNotNull()

    private fun Assert<Result<TemplatingError, StringBuilder>>.result() =
        transform { it.get().toString() }
}