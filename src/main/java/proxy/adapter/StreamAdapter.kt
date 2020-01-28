package proxy.adapter

import org.reactivestreams.Publisher
import java.lang.reflect.Type

interface StreamAdapter<T, out R> {
    fun adapt(stream: Stream<T>): R
    interface Factory {
        fun create(type: Type): StreamAdapter<Any, Any>
    }
}

/**
 * An sequence of asynchronous values of type `T` that is used to emit [WebSocket.Event] and [Message].
 *
 * A Stream is lazy. It may be started by [start].
 */
interface Stream<T> : Publisher<T> {

    fun start(observer: Observer<T>): Disposable

    /**
     * Observes a [Stream].
     */
    interface Observer<in T> {
        /**
         * Data notification.
         */
        fun onNext(data: T)

        /**
         * Failed terminal state.
         */
        fun onError(throwable: Throwable)

        /**
         * Successful terminal state.
         */
        fun onComplete()
    }

    interface Disposable {
        fun dispose()

        fun isDisposed(): Boolean
    }
}
