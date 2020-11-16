package at.mikemitterer.template.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 *
 *
 * @since   06.11.20, 13:24
 */
@ExperimentalCoroutinesApi
class CoroutinesTest {
    private val logger = LoggerFactory.getLogger(CoroutinesTest::class.java)
    private val log: (msg: String) -> Unit = { msg ->
        logger.info(
            "    [${Thread.currentThread().toString().padEnd(70)}] $msg"
        )
    }

    /**
     * runBlocking blockt den Main-Thread
     */
    @Test
    fun testLaunche() = runBlocking<Unit> {
        logger.info("Context testLaunche: $coroutineContext")
        val countTo: suspend (name: String, limit: Int) -> Unit = { name, limit ->
            for (i in 1..limit) {
                log("$name - Counter: $i")
                delay(100)
            }
        }

        // Blockt nicht
        launch {
            logger.info("Context Block1: $coroutineContext")
            countTo("Lauch 1", 5)
        }

        logger.info("---- 3 ----")

        launch {
            logger.info("Context Block2: $coroutineContext")
            countTo("Lauch 2", 5)
        }

        async {
            logger.info("Context Async: $coroutineContext")
            countTo("Async 3", 5)
        }.await()

        runBlocking {
            logger.info("Context testLaunche/runBlocking: $coroutineContext")

            launch {
                logger.info("Context testLaunche/runBlocking/launch: $coroutineContext")
                countTo("Lauch 4/1", 5)
            }

            launch(Dispatchers.IO) {
                logger.info("Context testLaunche/runBlocking/launch: $coroutineContext")
                countTo("Lauch 5/1", 5)
            }

            async {
                logger.info("Context  testLaunche/runBlocking/async: $coroutineContext")
                countTo("Async 6/1", 5)
            }.await()
        }

        launch {
            logger.info("Context Block3: $coroutineContext")
            countTo("Lauch 7", 5)
        }
    }

    /**
     * YT-Video: https://youtu.be/k_xRxXoimSw
     */
    @Test
    internal fun testCoroutineScope() = runBlocking {
        val networkRequest: suspend () -> Unit = {
            val time = measureTimeMillis {
                val job = GlobalScope.launch(Dispatchers.IO) {
                    // wird durch job.cancel NICHT beeinflusst da eben
                    // alles geblockt wird
                    runBlocking {
                        log("Start runBlocking...")
                        delay(1000)
                        log("runBlocking...")
                        delay(1000)
                    }

                    // Wird nicht ausgeführt da der Job gecancelt wurde!
                    coroutineScope {
                        log("Start coroutineScope...")
                        delay(1000)
                        log("coroutineScope...")
                        delay(1000)
                    }
                }

                // Delay braucht es nicht mal - runBlocking wird ausgeführt
                // coroutineScope sowieso nicht mehr
                delay(400)

                log("Cancelling job...")
                job.cancel()

                job.join()
            }
            logger.info("networkRequest took ${time}ms")
        }

        networkRequest()
    }

    /**
     * YT-Video: https://youtu.be/duGTtbDMML0
     */
    @Test
    internal fun testContextAndDispatchers() = runBlocking {
        // Verwendet den Main-Thread
        //
        // Launch erbt den Scope im von dem Parent in dem
        // die neue Coroutine gestartet wurde
        launch {
            log("main runBlocking")
        }
        delay(1000)

        // Auch Main-Thread
        launch(Dispatchers.Unconfined) {
            log("Unconfined (before delay) Context: [$coroutineContext]")
            delay(100)
            // Bei Unconfined wird hier der Thread von delay übernommen
            log("Unconfined (after delay)  Context: [$coroutineContext]")
        }
        delay(1000)

        // Worker-Thread
        launch(Dispatchers.Default) {
            log("Default (before delay) Context: [$coroutineContext]")
            delay(100)
            // Bei Default bleibt der Thread gleich...
            log("Default (after delay)  Context: [$coroutineContext]")
        }
        delay(1000)

        // Worker-Thread
        launch(Dispatchers.IO) {
            log("IO")
        }
        delay(1000)

        // Erstellt einen eigenen Thread (Obsolete!!!!!!!!)
        // launch(newSingleThreadContext("Banana Thread")) {
        //     log("Banana Thread")
        // }
        // delay(1000)
    }

    @Test
    fun testWithTimeout() = runBlockingTest {
        val signal = CountDownLatch(1)

        try {
            withTimeout(1000) {
                launch {
                    repeat(10) {
                        logger.info("Sleeping...")
                        delay(200)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            signal.countDown()
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        assertThat(signal.await(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun testExceptionInLaunch() = runBlockingTest {
        val signal = CountDownLatch(1)
        var foundException = false

        launch {
            try {
                delay(200)
                throw IllegalArgumentException("Crash nach 200ms")
            } catch (e: Exception) {
                logger.info("Found Exception! (${e})")
                foundException = true
                signal.countDown()
            }
        }

        withTimeout(3000) {
            val job = async {
                for (i in 1..10) {
                    if (foundException) {
                        return@async true
                    }
                    delay(300)
                }
                false
            }
            val result = job.await()
            assertThat(result).isTrue()
        }
    }

    @Test
    fun testExceptionInLaunch2() = runBlocking<Unit> {
        var foundException = false

        var job: Job? = null

        job = launch {
            try {
                delay(200)
                throw IllegalArgumentException("Crash nach 200ms")
            } catch (e: IllegalArgumentException) {
                logger.info("Found Exception! (testExceptionInLaunch2/${e})")
                foundException = true
            }

        }

        job.join()

        assertThat(foundException).isTrue()
        assertThat(job.isCancelled).isFalse()
    }

    /**
     * Funktioniert nur mit runBlockingTest - nicht mit runBlocking<Unit>
     */
    @Test
    fun testExceptionInAsync() = runBlockingTest {
        var foundException = false

        val job = async<Boolean> {
            delay(500)
            throw IllegalArgumentException("Crash nach 500ms")
        }

        try {
            job.await()
        } catch (e: IllegalArgumentException) {
            logger.info("Found Exception! (testExceptionInAsync/${e})")
            foundException = true
        }

        assertThat(foundException).isTrue

        // Im Gegensatz zu "launch" cancelt async
        assertThat(job.isCancelled).isTrue
    }
}