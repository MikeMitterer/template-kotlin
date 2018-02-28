package at.mikemitterer.template

import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory

/**
 * @since 28.02.18, 09:38
 */
class GreeterTest : Assert() {
    private val logger = LoggerFactory.getLogger(GreeterTest::class.java)
    
    @Test
    fun testGreeter() {
        val greeter = Greeter("Mike")

        logger.info(greeter.message)
        assertEquals("Hello, Mike!",greeter.message)
    }
}
