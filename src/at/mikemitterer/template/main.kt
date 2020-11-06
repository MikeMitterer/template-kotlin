package at.mikemitterer.template

class Greeter(private val name: String) {
    fun greet() {
        println(message)
    }

    val message: String
        get() = "Hello, $name!"
}

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        Greeter(args[0]).greet()
    } else {
        println("Not cmdline-argument available!")
    }
}