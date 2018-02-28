package at.mikemitterer.template

import org.junit.Assert
import org.junit.Test

/**
 * @since 28.02.18, 09:38
 */
class GreeterTest : Assert() {
    @Test
    fun testGreeter() {
        val greeter = Greeter("Mike")
        assertEquals("Hello, Mike!",greeter.message)
    }
}
