package resultk.demo.promotedusage

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import resultk.*
import resultk.demo.promotedusage.PromotesUsageDemonstrationTest.GetBundleError.BundleNotPresent
import resultk.demo.promotedusage.PromotesUsageDemonstrationTest.GetBundleError.KeyIsMissing
import resultk.testing.assertions.isFailureResult
import resultk.testing.assertions.isSuccessResult
import java.util.*


@DisplayName("Promoted usage demonstration test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class PromotesUsageDemonstrationTest {

    @Test
    @Order(1)
    fun `Upfront error handling - do not do this`() {
        val message = kotlin.runCatching {
            // oops,
            if (System.currentTimeMillis() > 0) throw OutOfMemoryError()
            ResourceBundle.getBundle("messages").getString("message.key")
        }.getOrElse { it.printStackTrace(); null }
        assertThat(message).isNull()
        // Why:
        //  1. Could catch something you cannot and should not handle.
        //  2. Now the message is null, still do not know why it was handled
        //  3. It is not even logged!
        //  4. No way of knowing if the message bundle does not exists, or the message key!
    }

    @ParameterizedTest
    @CsvSource(
        nullValues = ["@null"],
        value = [
            "resultk/demo/Messages,customerMessageX,KeyIsMissing",
            "resultk/demo/Messages6421,customerMessageX,BundleNotPresent",
            "resultk/demo/Messages,customerMessage1,@null"
        ],
    )
    @Order(2)
    fun `Upfront error handling - a better way`(bundle: String, key: String, expectedErrorKind: String?) {

        val expectedErrorClass = expectedErrorKind?.let {
            GetBundleError::class.sealedSubclasses.first { errorClass ->
                errorClass.simpleName == expectedErrorKind
            }
        }

        val noSuchMessageBundleError = { _: MissingResourceException -> BundleNotPresent(bundle) }
        val keyNotPresentInBundleError = { _: MissingResourceException -> KeyIsMissing(bundle, key) }
        val message: Result<GetBundleError, String> =
            resultWithHandlingOf(noSuchMessageBundleError) { ResourceBundle.getBundle(bundle).success() }
                .thenResultOfHandling(keyNotPresentInBundleError) { result.getString(key).success() }
                .onFailure(::println)
                .onSuccess(::println)

        if (expectedErrorClass == null) {
            assertThat(message)
                .isSuccessResult()
        } else {
            assertThat(message)
                .isFailureResult().isInstanceOf(expectedErrorClass)
        }

        // Why?
        //  1. Only handle exceptions which are relevant to this use case, nothing else
        //  2. Decide how to handle exceptions before deciding to make the call which produces it (pro-active deal with alternative flows).
        //  3. Distinguish between resource bundle missing and message key missing.
        //  4. Error codes provide enough information to the caller to act on what is relevant.
        //  5. Error codes captures information specific to the error: Missing key + bundle VS Missing bundle (do not care about the key!).
        //  6. Keep the happy flow here -- it is up the caller to crash the party 💃!
        //  7. Sealed class, just like enums, presents a finite variable number of error types.
    }

    sealed class GetBundleError {

        abstract val bundleName: String
        abstract val message: String

        override fun toString(): String = "[${javaClass.simpleName}]: $message"

        class BundleNotPresent(
            override val bundleName: String,
            override val message: String = "Bundle not present: $bundleName"
        ) : GetBundleError()

        class KeyIsMissing(
            override val bundleName: String,
            val missingKey: String,
            override val message: String = "Key [$missingKey] not present in bundle: [$bundleName]"
        ) : GetBundleError()
    }

}