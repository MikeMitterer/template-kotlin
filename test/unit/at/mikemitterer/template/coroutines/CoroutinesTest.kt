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

    @Test
    fun testNamingCoroutineThings() {
        val dispatcher = Dispatchers.IO

        // Blockt den Thread in dem sie aufgerufen wurde
        runBlocking<Unit> {
            println("[1] My context is: $coroutineContext")
            println("[1] My Job is: ${coroutineContext.job}")
        }

        // launch, async usw. erstellen eine Coroutine, benötigen
        // dazu einen Scope
        val job = CoroutineScope(dispatcher).launch {
            println("[2] My context is: $coroutineContext")
        }
        // Darf nur innerhalb eine coroutine oder innerhalb einer suspend-Funktion aufgerufen werde
        // job.join()

        // GlobalScope is a instance of scope that is global
        GlobalScope.launch(Dispatchers.Default) {
            println("[3] My context is: ${coroutineContext}")
        }

        // KA was das ist?????
        with(CoroutineScope(dispatcher)) {
            println("[4] My context is: $coroutineContext.")
        }

        runBlocking {
            // coroutineScope ist eine globale suspend-Funktion die einen
            // neuen CoroutineScope erstellt und wartet bis die Funktion selbst und
            // alle alle Child-Suspend-Functions beendet sind.
            //
            // coroutineScope ist eine suspend-Funktion und kann nicht außerhalb eine
            // Coroutine aufgerufen werden
            coroutineScope { // Creates a new coroutine scope
                launch {
                    delay(500L)
                    println("Task from nested launch")
                }

                delay(100L)
                println("Task from coroutine scope") // This line will be printed before nested launch
            }
        }

        // Produziert folgenden output:
        //     Vor convention-Function
        //         Inside launch - vor convention-Function
        //             [1] Inside 'conventionCallingSuspendFunction' waiting 1000ms...
        //             [1] 'conventionCallingSuspendFunction' finished!
        //         Inside launch - NACH convention-Function
        //     Nach convention-Function
        runBlocking {
            println("Vor convention-Function")
            // val job2 = CoroutineScope(Dispatchers.Default).launch {
                println("    Inside launch - vor convention-Function")
                conventionCallingSuspendFunction(1)
                println("    Inside launch - NACH convention-Function")
            // }
            println("Nach convention-Function")
            // job.join()
        }

        // Produziert folgenden output:
        //     Vor convention-Function
        //         Inside launch - vor convention-Function
        //         Inside launch - NACH convention-Function
        //     Nach convention-Function
        //                 [1] Inside 'conventionCallingSuspendFunction' waiting 1000ms...
        //                 [2] Inside 'conventionCallingSuspendFunction' waiting 1000ms...
        //                 [1] 'conventionCallingSuspendFunction' finished!
        //                 [2] 'conventionCallingSuspendFunction' finished!

        runBlocking {
            println("Vor convention-Function")
            // val job2 = CoroutineScope(Dispatchers.Default).launch {
            println("    Inside launch - vor convention-Function")
            launch {
                conventionCallingSuspendFunction(1)
            }
            launch {
                conventionCallingSuspendFunction(2)
            }
            println("    Inside launch - NACH convention-Function")
            // }
            println("Nach convention-Function")
            // job.join()
        }


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
    fun testExceptionInAsync() = runBlocking<Unit> {
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

    @Test
    fun testWaitForLaunchToBeFinished() = runBlocking<Unit> {
        logger.info("Start...")
        val time = measureTimeMillis {
            // Die beiden launch müssen fertig werden bevor
            // coroutineScope fertig wird
            coroutineScope {
                launch {
                    repeat(10) {
                        delay(200)
                        logger.info("Loop1")
                    }
                }

                launch {
                    repeat(10) {
                        delay(200)
                        logger.info("Loop2")
                    }
                }
            }
        }
        logger.info("Took ${time}ms, Ende...")
        assertThat(time).isBetween(2000, 2100)
    }
}

suspend fun conventionCallingSuspendFunction(id: Int) = withContext(Dispatchers.Default) {
    println("       [$id] Inside 'conventionCallingSuspendFunction' waiting 1000ms...")
    delay(1000)
    println("       [$id] 'conventionCallingSuspendFunction' finished!")
}