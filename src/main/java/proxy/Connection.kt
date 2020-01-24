package proxy

import proxy.adapter.model.Message

class Connection {

    private val messages = mutableListOf<Message>()

    fun send(message: Message): Boolean {
        messages.add(message)
        return true
    }

    fun observeEvents(): List<Message> {
        return messages
    }
}