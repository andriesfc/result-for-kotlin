package resultk.modelling.i8n

import kotlin.test.Test


internal class I8nTest {

    private val messages = I8nMessages("resultk/modelling/i8n/I8nTestMessages")

    @Test
    fun testKeys() {
        println(messages)
        println(messages.build("greeting.morning", "user" to "andries").get())
    }
}