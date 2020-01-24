package proxy

fun main() {
    val scarlet = Scarlet.Builder().build()
    val service = scarlet.create<Task>()

    service.sendMessage("hello world")

    service.observeEvents().forEach {
        println(it)
    }
}
