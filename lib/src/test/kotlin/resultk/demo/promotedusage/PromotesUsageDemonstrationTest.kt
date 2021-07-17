package resultk.demo.promotedusage

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.onFailure
import resultk.resultCatching
import resultk.success
import resultk.testing.assertions.isFailureResult
import resultk.demo.promotedusage.PromotesUsageDemonstrationTest.GetBundleError.BundleNotPresent
import resultk.demo.promotedusage.PromotesUsageDemonstrationTest.GetBundleError.KeyIsMissing
import resultk.thenResultCatching
import java.util.*


@DisplayName("Promoted usage demonstration test")
internal class PromotesUsageDemonstrationTest {

    @Test
    fun `Upfront error handling - do not do this`() {
        val message = kotlin.runCatching { ResourceBundle.getBundle("messages").getString("message.key") }.getOrNull()
        assertThat(message).isNull()
        // Why:
        //  1. Could catch something you cannot and should not handle.
        //  2. Now the message is null, still do not know why it was handled
        //  3. It is not even logged!
        //  4. No way of knowing if the message bundle does not exists, or the message key!
    }

    @Test
    fun `Upfront error handling - a better way`() {

        val bundle = "testing_messages_bundle_645"
        val key = "test_key"
        val message =
            resultCatching<MissingResourceException, GetBundleError, ResourceBundle>({ BundleNotPresent(bundle) }) {
                ResourceBundle.getBundle(bundle).success()
            }.thenResultCatching<MissingResourceException, GetBundleError, ResourceBundle, String>({
                KeyIsMissing(
                    bundle,
                    key
                )
            }) {
                value.getString(key).success()
            }.onFailure(::println)

        assertThat(message)
            .isFailureResult()
            .isInstanceOf(BundleNotPresent::class)

        // Why?
        //  1. Only handle exceptions which are relevant to this use case, nothing else
        //  2. Decide how to handle exceptions before deciding to make the call which produces it (pro-active deal with alternative flows).
        //  3. Distinguish between resource bundle missing and message key missing.
        //  4. Error codes provide enough information to the caller to act on what is relevant.
        //  5. Error codes captures information specific to the error: Missing key + bundle VS Missing bundle (do not care about the key!).
        //  6. Keep the happy flow here -- it is up the caller to crash the party 💃!
        //  7. Sealed class, just like enums presents finite, variable number of error conditions and types.
    }

    sealed class GetBundleError {

        abstract val bundleName: String
        abstract val message: String

        data class BundleNotPresent(
            override val bundleName: String,
            override val message: String = "$bundleName not present"
        ) : GetBundleError()

        data class KeyIsMissing(
            override val bundleName: String,
            val missingKey: String,
            override val message: String = "Key [$missingKey] not present in bundle: [$bundleName]"
        ) : GetBundleError()
    }

}