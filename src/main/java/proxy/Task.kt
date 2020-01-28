package proxy

import io.reactivex.Flowable

interface Task {
    @Send
    fun sendMessage(message: String)

    @Receive
    fun observeEvents(): Flowable<String>
}