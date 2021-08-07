package resultk.internal

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import resultk.testing.assertions.messageKeys
import java.util.*

@DisplayName("Test access to messages resource used internally")
internal class InternalMessagesTest {

    private val requiredMessageKeys: Set<String> = setOf(testOnlyKey) + arrayOf(
        "error.noSuchValueInOption"
    )

    private lateinit var resourceBundle: ResourceBundle

    @BeforeAll
    fun setupAll() {
        resourceBundle = ResourceBundle.getBundle(INTERNAL_MESSAGE_RESOURCE_BUNDLE)
    }

    @Test
    fun `At least the 'testOnly' key exists in message bundle`() {
        assertThat { resourceBundle.getString(testOnlyKey) }
            .isSuccess()
            .isNotEmpty()
    }

    @Test
    fun `All required message keys are present and have at least a message associated with each`() {
        assertThat(resourceBundle).messageKeys().all {
            containsAll(*requiredMessageKeys.toTypedArray())
            each { messageKey ->
                messageKey.transform { resourceBundle.getString(it) }
                    .isNotEmpty()
            }
        }
    }

    companion object {
        private const val testOnlyKey = "testOnlyKey"
    }

}

