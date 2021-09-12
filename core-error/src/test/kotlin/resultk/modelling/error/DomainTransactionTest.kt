package resultk.modelling.error

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resultk.modelling.testing.assertions.peek
import resultk.modelling.testing.atTimeNow
import resultk.modelling.testing.fixtures.StripeTransactionError
import resultk.modelling.testing.new
import java.time.*
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DomainTransactionTest {

    @ParameterizedTest
    @MethodSource("testableDomainErrors")
    fun errorShouldAlwaysHaveMessage(error: StripeTransactionError) {
        assertThat { error.message }
            .isSuccess().peek { println("${error.errorCode}: $it") }
            .isNotEmpty()
    }


    @ParameterizedTest
    @MethodSource("testableDomainErrors")
    fun errorShouldHaveDebugMessage(err: StripeTransactionError) {
        assertThat(err.debugMessage).isNotNull().peek(::println).given { debug ->
            assertThat(debug).contains(err.traceId)
            assertThat(debug).contains(err.date.toString())
        }
    }

    private fun testableDomainErrors(): List<StripeTransactionError> {
        return StripeTransactionError::class.sealedSubclasses.map { t ->
            t.new(*trace())
        }
    }

    private val trace = object : Iterator<Array<Pair<String, Any?>>> {
        private var next =
            Trace(LocalTime.now().atDate(LocalDate.of(2021, Month.JUNE, 16)))

        override fun hasNext(): Boolean = true
        override fun next() = next.toArgs().also { next = next.next() }
    }::next


    private data class Trace(
        val date: LocalDateTime,
        val traceId: String = buildString {
            append(date.toInstant(ZoneOffset.UTC).epochSecond.toString(16))
            append(UUID.randomUUID().toString().replace("-", ""))
            forEachIndexed { index, c -> set(index, c.uppercaseChar()) }
        }
    ) {
        fun next() = copy(date = date.atTimeNow())
        fun toArgs(): Array<Pair<String, Any?>> = arrayOf("traceId" to traceId, "date" to date)
    }

}
