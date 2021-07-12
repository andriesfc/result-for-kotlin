package resultk.demo.acmepayments

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.*
import resultk.failure
import java.util.*
import java.util.ResourceBundle.getBundle

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentProcessorErrorTest {

    private val testLocale = Locale.ENGLISH
    private lateinit var upstreamErrorCode: String
    private lateinit var upstreamErrorProvider: String
    private var upstreamProviderErrorMessage: String? = null
    private lateinit var upstreamError: PaymentProcessorError.UpstreamError
    private lateinit var actualDefaultLocale: Locale


    @BeforeAll
    fun setupAll() {
        actualDefaultLocale = Locale.getDefault()
        Locale.setDefault(testLocale)
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
    fun errorCodedAsEnumLikeAndClassLike() {

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
    fun resourceBundleIsFullyMapped() {
        val expectedMappedMessageKeys =
            PaymentProcessorError.constants.map(PaymentProcessorError::messageKey).toTypedArray()
        println("Available Message Keys:")
        println("-------------------------------------------------------------------------------------------------")
        expectedMappedMessageKeys.forEachIndexed { index, messageKey -> println("${index + 1}. $messageKey") }
        println()
        assertThat { getBundle(PaymentProcessorError.PAYMENT_PROCESSOR_MESSAGES) }
            .isSuccess()
            .given { actualResourceBundle ->
                val actualResourceKeys = actualResourceBundle.keys.toList()
                assertThat(actualResourceKeys).containsAll(* expectedMappedMessageKeys)
            }
    }

    @Test
    fun catersForUpstreamErrorsWithNoSpecialMappedCodes() {

        val genericUpstreamErrorMessage = getBundle(PaymentProcessorError.PAYMENT_PROCESSOR_MESSAGES)
            .getString("error.upstream")

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
                    contains(unMappedUpstreamError.upstreamProvider)
                    contains(unMappedUpstreamError.upstreamProviderErrorMessage!!)
                    contains(unMappedUpstreamError.upstreamErrorCode)
                }
            }
    }

    @Test
    fun throwsCustomErrorIfCallerDoesHandleErrorCase() {
        val paymentCompletionId = PaymentProcessorError.PaymentDeclined.failure<PaymentProcessorError, Long>()

        paymentCompletionId.runCatching { get() }.onFailure { ex ->
            ex.printStackTrace()
        }

        assertThat { paymentCompletionId.get() }.isFailure().isInstanceOf(PaymentProcessorException::class)

    }

}
