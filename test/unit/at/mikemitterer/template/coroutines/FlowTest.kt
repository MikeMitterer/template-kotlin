package at.mikemitterer.template.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 *
 *
 * @since   09.11.20, 09:11
 */
@ExperimentalCoroutinesApi
class FlowTest {
    private val logger = LoggerFactory.getLogger(FlowTest::class.java)

    @Test
    fun testFlow() = runBlocking {
        val namesFlow = listOf("Jody", "Steve", "Lance", "Joe").asFlow()

        namesFlow
                .map { name -> name.length }
                .filter { length -> length < 5 }
                .collect {
                    println(it)
                }
    }

    @Test
    fun testDataStream() = runBlocking<Unit> {
        val numbers =  MutableSharedFlow<Int>()
        launch {
            var counter = 0
            repeat(50) {
                numbers.emit(counter++)
                delay(100)
            }
        }

        var firstNumber = -1
        val job = async {
            delay(1000)
            numbers.asSharedFlow()
                    .takeWhile { number ->
                        // logger.info("while Number < 10! ($number)" )
                        number <= 10
                    }
                    .collect { number ->
                        logger.info("Number (Job1): $number" )
                        if(firstNumber == -1) {
                            firstNumber = number
                        }
                        delay(400)
            }
        }

        launch {
            numbers.asSharedFlow()
                    .takeWhile { number ->
                        // logger.info("while Number < 45! ($number)" )
                        number <= 45
                    }
                    .collect { number ->
                        logger.info("Number (Job2): $number" )
                        delay(300)
                    }
        }

        // Der Timer bricht den Job ab wenn nicht innerhalb von
        // 5secs ein Event (nummer) kommt
        val timeout = object : TimerTask() {
            override fun run() {
                logger.warn("Job canceled!")
                job.cancel()
            }
        }
        Timer().schedule(timeout, 5000)
        job.await()
        // job2.await()

        assertThat(firstNumber).isEqualTo(4)
        timeout.cancel()
    }
    @ExperimentalTime
    @Test
    fun testDataStreamHighToLow() = runBlockingTest {
        val numbers =  MutableSharedFlow<Int>()
        val emitterJob = launch {
            var counter = 50
            repeat(30) {
                numbers.emit(counter--)
                logger.info("Emitting: $counter...")
                delay(800)
            }
        }

        val job: Deferred<Unit>?

        // Der Timer bricht den Job ab wenn nicht innerhalb von
        // 5secs ein Event (nummer) kommt
        val timeout = object : TimerTask() {
            override fun run() {
                logger.warn("Job canceled!")
                // job?.cancel()
                emitterJob.cancel()
            }
        }
        Timer().schedule(timeout, 500)

        var firstNumber = -1
        job = async {
            // delay(1000)
            numbers.asSharedFlow()
                    .takeWhile { number ->
                        logger.info("while Number < 10! ($number)" )
                        number <= 10
                    }
                    .collect { number ->
                        logger.info("Number (Job1): $number" )
                        if(firstNumber == -1) {
                            firstNumber = number
                        }
                        delay(100)
            }
        }

        logger.info("Waiting for Job to finish...")
        val time = measureTime {
            job.await()
        }
        logger.info("Job finished, took ${time}ms")
        // job2.await()

        assertThat(firstNumber).isEqualTo(-1)
        // timeout.cancel()
    }

    /**
     * Ziel ist diese Ausgabe:
     *     Number: 1
     *     Number: 2
     *     Number: 1
     *     Number: 3
     */
    @FlowPreview
    @Test
    fun testDebounce()  = runBlockingTest {
        val numberFlow = listOf(1,2,1,1,3).asFlow()
        var prevValue = -1

        numberFlow
            .transform { value ->
                if(value != prevValue) {
                    prevValue = value
                    return@transform emit(value)
                }
            }
            .collect {
                println("Number: $it")
            }
    }
}
