package resultk.modelling.demos

import assertk.assertThat
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test
import resultk.modelling.testing.assertions.peek
import resultk.raise

internal class TransactionErrorTest {

    @Test
    fun testRaisingAsException() {
        println(TransactionError.StopEndOfFile)
        assertThat { raise(TransactionError.StopEndOfFile) }
            .isFailure()
            .isInstanceOf(TransactionError.TransactionException::class)
            .peek(TransactionError.TransactionException::printStackTrace)
    }

}