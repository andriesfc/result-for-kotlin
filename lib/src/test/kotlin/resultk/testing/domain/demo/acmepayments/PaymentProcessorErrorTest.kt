@file:Suppress("SameParameterValue")

package resultk.testing.domain.demo.acmepayments

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.*
import resultk.testing.domain.demo.acmepayments.PaymentProcessorError.Companion.PAYMENT_PROCESSOR_MESSAGES
import resultk.failure
import java.util.*
import java.util.ResourceBundle.getBundle

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Acme Payment ProcessorError tests")
internal class PaymentProcessorErrorTest {

    private val knownUpstreamProvider = "moon68inc"
    private val knownUpstreamProviderErrorCode = "E_660-011"
    private val testLocale = Locale.ENGLISH

    private lateinit var upstreamErrorCode: String
    private lateinit var upstreamErrorProvider: String
    private var upstreamProviderErrorMessage: String? = null
    private lateinit var upstreamError: PaymentProcessorError.UpstreamError
    private lateinit var actualDefaultLocale: Locale
    private lateinit var resourceBundle: ResourceBundle
    private lateinit var resourceBundleKeys: LinkedHashSet<String>

    @BeforeAll
    fun setupAll() {
        actualDefaultLocale = Locale.getDefault()
        Locale.setDefault(testLocale)
        resourceBundle = getBundle(PAYMENT_PROCESSOR_MESSAGES)
        resourceBundleKeys = LinkedHashSet(resourceBundle.keys.toList())
    }

    @AfterAll
    fun tearDownAll() {
        Locale.setDefault(actualDefaultLocale)
    }

    @BeforeEach
    fun setUp() {
        upstreamErrorCode = "upstream_error_1"
        upstreamErrorProvider = "upstream_error_provider_1"
        upstreamProviderErrorMessage = "upstream_error_1_message"
        upstreamError = PaymentProcessorError.UpstreamError(
            upstreamProvider = upstreamErrorProvider,
            upstreamErrorCode = upstreamErrorCode,
            upstreamProviderErrorMessage = upstreamProviderErrorMessage
        )
    }


    @Test
    fun `Error coded as enum like and class like`() {

        val allErrorCodes = (PaymentProcessorError.constants + upstreamError).toSet()

        allErrorCodes.forEachIndexed { index, error ->
            println("${index + 1}:\t$error")
        }

        assertThat(allErrorCodes.sortedBy(PaymentProcessorError::code)).containsExactly(
            PaymentProcessorError.BlackedListenerPermanently,
            PaymentProcessorError.InsufficientFunds,
            PaymentProcessorError.PaymentDeclined,
            upstreamError
        )

    }

    @Test
    fun `Resource bundle is fully mapped`() {
        val expectedMappedMessageKeys =
            PaymentProcessorError.constants.map(PaymentProcessorError::messageKey).toTypedArray()
        println("Available Message Keys:")
        println("-------------------------------------------------------------------------------------------------")
        expectedMappedMessageKeys.forEachIndexed { index, messageKey -> println("${index + 1}. $messageKey") }
        println()
        assertThat(resourceBundle)
            .transform("resourceBundle.key") { it.keys.toList() }
            .containsAll(* expectedMappedMessageKeys)
    }

    @Test
    fun `Caters for upstream errors with no special mapped codes`() {

        val genericUpstreamErrorMessage = bundledMessage("error.upstream")

        val unMappedUpstreamError = PaymentProcessorError.UpstreamError(
            upstreamProvider = "new_upstream_provider_701",
            upstreamErrorCode = "E_342",
            upstreamProviderErrorMessage = "Unconfigured down stream requestor"
        )

        println("code    = ${unMappedUpstreamError.code}")
        println("message = ${unMappedUpstreamError.message()}")

        assertThat { unMappedUpstreamError.message() }
            .isSuccess()
            .given { actual ->
                assertThat(actual).all {
                    contains(genericUpstreamErrorMessage)
                    contains(unMappedUpstreamError.upstreamErrorCode)
                    contains(unMappedUpstreamError.upstreamProvider)
                    when (val upstreamProviderMessage = unMappedUpstreamError.upstreamProviderErrorMessage) {
                        null -> contains(bundledMessage("sentence_building.not_supplied_by_upstream_provider"))
                        else -> contains(upstreamProviderMessage)
                    }
                }
            }
    }

    @Test
    fun `Throws custom error if caller does handle error case`() {
        val paymentCompletionId = PaymentProcessorError.PaymentDeclined.failure<PaymentProcessorError, Long>()

        paymentCompletionId.runCatching { get() }.onFailure { ex ->
            ex.printStackTrace()
        }

        assertThat { paymentCompletionId.get() }.isFailure().isInstanceOf(PaymentProcessorException::class)

    }

    @Test
    fun `Caters for known upstream error codes`() {
        val upstreamError = PaymentProcessorError.UpstreamError(knownUpstreamProvider, knownUpstreamProviderErrorCode)
        println(upstreamError.message())
        assertThat(resourceBundleKeys, PAYMENT_PROCESSOR_MESSAGES).contains(upstreamError.messageKey)
        assertThat(upstreamError).prop("errorCode") { it.upstreamErrorCode }.isEqualTo(knownUpstreamProviderErrorCode)
        assertThat(upstreamError).prop("upstreamProviderCode") { it.upstreamProvider }.isEqualTo(knownUpstreamProvider)
    }

    @Test
    fun `All constant errors are have mapped message keys`() {
        val expectedMessageKeys = PaymentProcessorError.constants.map(PaymentProcessorError::messageKey).sorted()
        assertThat(resourceBundleKeys).containsAll(* expectedMessageKeys.toTypedArray())
    }

    companion object {
        private fun bundledMessage(key: String, vararg args: Any?): String {
            return getBundle(PAYMENT_PROCESSOR_MESSAGES).getString(key).run {
                when {
                    args.isEmpty() -> this
                    else -> format(* args)
                }
            }
        }
    }
}
