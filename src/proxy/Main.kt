package proxy

fun main() {
    val task = ProxyFactory.Builder().build()
    val service = task.create<Task>()

    service.setData("Test")
    println(service.getCalData(5))
}