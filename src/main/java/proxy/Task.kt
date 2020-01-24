package proxy

interface Task {
    @Send
    fun sendMessage(message: String)

    @Receive
    fun observeEvents(): List<String>
}