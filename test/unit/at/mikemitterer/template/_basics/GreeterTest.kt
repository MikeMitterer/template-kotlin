package at.mikemitterer.template._basics

import at.mikemitterer.template.Greeter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals

/**
 * @since 28.02.18, 09:38
 */
class GreeterTest  {
    private val logger = LoggerFactory.getLogger(GreeterTest::class.java)
    
    @Test
    fun testGreeter() {
        val greeter = Greeter("Mike")

        logger.info(greeter.message)
        assertEquals("Hello, Mike!",greeter.message)
    }

    @Test
    fun testGreeterWithAssertJ() {
        val greeter = Greeter("Mike")

        logger.info(greeter.message)
        assertThat("Hello, Mike!").isEqualTo(greeter.message)
    }


}
