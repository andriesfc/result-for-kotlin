package resultk.modelling.error

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import assertk.assertions.startsWith
import org.junit.jupiter.api.Test
import resultk.modelling.testing.assertions.peek
import resultk.modelling.testing.fixtures.StripeError
import resultk.modelling.testing.fixtures.StripeTransactionErrorEnum


class LocalisedMessageProviderTest {

    @Test
    fun testAbilityToOverrideMessageKey() {
        val expectedMessage = "thisMessageIsAsExpected🤙🏻"
        val expectedMessageKeyPrefix = "prefixIt"
        var actualMessageKey: String? = null
        val provider = object : LocalizedErrorMessagesProvider<StripeError>(StripeError.keyBundle) {
            override fun messageKey(e: StripeError): String {
                return "$expectedMessageKeyPrefix.${e.errorCode}"
            }

            override fun buildErrorMessage(error: StripeError, messageKey: String): String {
                actualMessageKey = messageKey
                return expectedMessage
            }
        }
        assertThat { provider.getErrorMessage(StripeTransactionErrorEnum.ACCOUNT_INVALID) }
            .isSuccess().peek { println(it) }

        assertThat(actualMessageKey).isNotNull().startsWith(expectedMessageKeyPrefix)
    }


    @Test
    fun testGetMessageFromEnumProvider() {
        val provider = EnumBasedLocalisedMessageProvider(
            StripeError.keyBundle,
            StripeTransactionErrorEnum::class
        )

        assertThat { provider.getErrorMessage(StripeTransactionErrorEnum.AMOUNT_TOO_LARGE) }
            .isSuccess()
            .isNotNull()
            .peek(::println)
    }

}