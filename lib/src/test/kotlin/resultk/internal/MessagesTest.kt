package resultk.internal

import assertk.assertThat
import assertk.assertions.isSuccess
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Test access to messages resource used internally")
internal class MessagesTest {

    @Test
    fun `Test only key is present`() {
        assertThat { resourceMessage(testOnlyKey) }.isSuccess()
    }


    companion object {
        private const val testOnlyKey = "testOnlyKey"
    }
}

