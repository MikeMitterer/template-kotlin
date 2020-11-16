# Kotlin - Coroutines

Jede Coroutine hat ihren eigenen Context

Eine Coroutine ist z.b.

    runBlockingTest {
        println("My context is: $coroutineContext")
        
        // Hat den selben Context wie runBlocking
        launch { ... }
        
        // Hat den selben Context wie runBlocking
        launch { ... }
        
        // Hat den selben Context wie runBlocking
        async { ... }.await()
        
    }
    
    runBlocking<Unit> {
        println("My context is: $coroutineContext")
        
        // Bekommt einen eigenen Context
        launch { ... }
        
        // Bekommt einen eigenen Context
        launch { ... }
        
        // Bekommt einen eigenen Context
        async { ... }.await()
        
    }
    