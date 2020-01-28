package proxy

import io.reactivex.Flowable
import io.reactivex.Observable
import proxy.adapter.Stream
import proxy.adapter.StreamAdapter
import proxy.adapter.TextMessageAdapter
import proxy.adapter.factory.BuiltInMessageAdapterFactory
import java.lang.reflect.Type

fun main() {
    val scarlet = Scarlet.Builder()
        .addMessageAdapterFactory(BuiltInMessageAdapterFactory(TextMessageAdapter()))
        .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
        .build()
    val service = scarlet.create<Task>()

    service.sendMessage("hello world")

    service.observeEvents().subscribe({
        println(it)
    }, {
        println(it)
    })
}


class FlowableStreamAdapter<T> : StreamAdapter<T, Flowable<T>> {
    override fun adapt(stream: Stream<T>): Flowable<T> = Flowable.fromPublisher(stream)
}

class ObservableStreamAdapter<T> : StreamAdapter<T, Observable<T>> {
    override fun adapt(stream: Stream<T>): Observable<T> {
        return Observable.fromPublisher(stream)
    }
}

class RxJava2StreamAdapterFactory : StreamAdapter.Factory {
    override fun create(type: Type): StreamAdapter<Any, Any> = when (type.getRawType()) {
        Flowable::class.java -> FlowableStreamAdapter()
        Observable::class.java -> ObservableStreamAdapter()
        else -> throw IllegalArgumentException("$type is not supported by this StreamAdapterFactory")
    }
}
