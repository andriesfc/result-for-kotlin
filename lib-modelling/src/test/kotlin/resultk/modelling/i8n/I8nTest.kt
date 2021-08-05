package resultk.modelling.i8n

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import resultk.Result
import resultk.modelling.internal.templating.ResolveExpression.ByLookupFunction
import resultk.modelling.internal.templating.eval
import resultk.modelling.testing.assertions.peek
import resultk.modelling.testing.fixtures.Quote
import resultk.modelling.testing.fixtures.UserBean
import resultk.modelling.testing.require
import resultk.modelling.testing.resource
import java.time.LocalDate
import java.time.Month


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class I8nTest {

    private val baseName = "resultk/modelling/i8n/TestMessages"
    private val keyBundle = keyBundleOf(baseName)
    private val messagesBundle = messsagesBundle(baseName)

    private object GreetingModel {
        val user = UserBean(
            name = "John",
            joinedDate = LocalDate.of(2015, Month.AUGUST, 17)
        )
        val quote = Quote(
            text = "I have not failed. I’ve just found 10,000 ways that won’t work.",
            attribution = "Thomas A. Edison"
        )
    }

    @Nested
    inner class TestBuildMessage {

        private val baseMessagesResource = resource("/$baseName.properties")

        private val modelLookup: Map<String, Any> = with(GreetingModel) {
            mapOf(
                "user.name" to user.name,
                "user.joinedDatedAsText" to user.joinedDatedAsText,
                "quote.text" to quote.text,
                "quote.attribution" to quote.attribution
            )
        }

        private val expectedGreetings: Map<String, String> = listOf(
            "user.greeting.morning",
            "user.greeting.morning.rude",
            "user.greeting.morning.inspirational"
        ).associateWith { greetingKey ->
            val template = baseMessagesResource.require(greetingKey)
            val expectedMessage = template.eval(ByLookupFunction { field ->
                requireNotNull(modelLookup[field]) {
                    """
                Please insert value for [$field] lookup value in [modelLookup]
                """.trimIndent()
                }
            }).get().toString()
            expectedMessage
        }

        @Test
        fun buildMessageWithMap() {
            val greeting = "user.greeting.morning"
            val expected = expectedGreetings.require(greeting)
            assertThat(messagesBundle.buildMessageWithMap(greeting, modelLookup))
                .isSuccess()
                .isEqualTo(expected)
        }

        @Test
        fun buildMessageWithBean() {
            val greeting = "user.greeting.morning.rude"
            val expected = expectedGreetings.require(greeting)
            assertThat(messagesBundle.buildMessageWithBean(greeting, GreetingModel))
                .isSuccess()
                .isEqualTo(expected)
        }

        @Test
        fun buildMessageWithPairs() {
            val greeting = "user.greeting.morning.inspirational"
            val userName = "user.name" to "Adam"
            val quoteAttribution = "quote.attribution" to "Tailor"
            val quote = "quote.text" to "Sunshine early morning is a smile"
            val expected = "Good morning ${userName.second}. " +
                    "Here is little inspiration " +
                    "from ${quoteAttribution.second}: \"${quote.second}\""
            assertThat(
                messagesBundle.buildMessageWithKeyValues(
                    greeting,
                    quote,
                    userName,
                    quoteAttribution
                )
            ).isSuccess().isEqualTo(expected)
        }
    }
}

fun Assert<Result<I8nError, String>>.isSuccess(): Assert<String> =
    peek { println(it) }.isInstanceOf(Result.Success::class).transform { it.result as String }

fun Assert<Result<I8nError, String>>.isError(): Assert<I8nError> =
    peek { println(it) }.isInstanceOf(Result.Failure::class).transform { it.error as I8nError }

