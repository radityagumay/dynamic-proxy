package proxy

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy


fun main() {
    val scarlet = Scarlet.Builder()
        .addStream(RxJavaStreamImpl())
        .build()
    val service = scarlet.create<Service>()

    service.send(Complement("hello"))
    service.send("world")
    service.observeEvents().forEach {
        println(it)
    }
}


data class Complement(val message: String)

interface Service {
    fun send(message: String)
    fun observeEvents(): List<String>
}

class ServiceImpl(
    private val streams: List<Stream>
) : Service {

    override fun send(message: String) {
        streams.forEach { it.send(message) }
    }

    override fun observeEvents(): List<String> {
        val messages = mutableListOf<String>()
        streams.forEach { messages.addAll(it.observeEvents()) }
        return messages
    }
}


class Scarlet(
    private val streams: List<Stream>
) {
    fun <T> create(service: Class<T>) = implementation(service)

    inline fun <reified T> create() = create(T::class.java)

    private fun <T> implementation(service: Class<T>): T {
        val proxy = Proxy.newProxyInstance(
            service.classLoader,
            arrayOf(service),
            ScarletInvocationHandler(ServiceImpl(streams))
        )
        return service.cast(proxy)
    }

    class Builder {
        private var streams = mutableListOf<Stream>()

        fun addStream(stream: Stream): Builder = apply { streams.add(stream) }

        fun build() = Scarlet(streams)
    }
}

interface Stream {
    fun send(message: String)
    fun observeEvents(): List<String>
}

interface RxJavaStream : Stream

class RxJavaStreamImpl : RxJavaStream {
    private val messages = mutableListOf<String>()

    override fun send(message: String) {
        messages.add(message)
    }

    override fun observeEvents(): List<String> {
        return messages
    }
}

interface FlowStream : Stream

class FlowStreamImpl : FlowStream {
    private val messages = mutableListOf<String>()

    override fun send(message: String) {
        messages.add(message)
    }

    override fun observeEvents(): List<String> {
        return messages
    }
}

class ScarletInvocationHandler(
    private val service: Service
) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        try {
            if (method?.name == "send") {
                method.invoke(service, *args!!)
            } else {
                return method?.invoke(service)
            }
        } catch (e: InvocationTargetException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
        return null
    }
}