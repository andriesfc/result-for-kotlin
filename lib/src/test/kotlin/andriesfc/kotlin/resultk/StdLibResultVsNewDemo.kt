package andriesfc.kotlin.resultk

import org.junit.jupiter.api.Test
import java.io.IOException

class StdLibResultVsNewDemo {

    @Test
    fun stdLibUsage() {
        runCatching { throw IOException("test: Error reading data") }
            .onSuccess { println("received: $it") }
            .onFailure { println("failed: $it") }
    }

    @Test
    fun libUsage() {
        resultOf<IOException, String> { throw IOException("test: Error reading data") }
            .onSuccess { println("received: $it") }
            .onFailure { println("failed: $it") }
    }

}

