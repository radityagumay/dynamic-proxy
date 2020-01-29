package proxy

import io.reactivex.schedulers.Schedulers
import proxy.adapter.MessageAdapter
import proxy.adapter.StreamAdapter
import proxy.adapter.TextMessageAdapter
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class Scarlet private constructor(
    private val serviceFactory: Service.Factory
) {
    fun <T> create(service: Class<T>) = implementation(service)

    inline fun <reified T> create() = create(T::class.java)

    private fun <T> implementation(service: Class<T>): T {
        val serviceInstance = serviceFactory.create(service)
        val proxy = Proxy.newProxyInstance(
            service.classLoader,
            arrayOf(service),
            createInvocationHandler(serviceInstance)
        )
        return service.cast(proxy)
    }

    private fun createInvocationHandler(
        serviceInstance: Service
    ): InvocationHandler {
        return InvocationHandler { _, method, args ->
            serviceInstance.execute(method, args)
        }
    }

    class Builder {
        private val messageAdapterFactories = mutableListOf<MessageAdapter.Factory>()
        private val streamAdapterFactories = mutableListOf<StreamAdapter.Factory>()

        fun addMessageAdapterFactory(factory: MessageAdapter.Factory): Builder =
            apply { messageAdapterFactories.add(factory) }

        fun addStreamAdapterFactory(factory: StreamAdapter.Factory): Builder =
            apply { streamAdapterFactories.add(factory) }

        fun build() = Scarlet(createServiceFactory())

        private fun createServiceFactory(): Service.Factory {
            val dafaultScheduler = Schedulers.computation()
            val connection = Connection(dafaultScheduler)
            val textAdapter = TextMessageAdapter()
            val messageAdapterResolver = MessageAdapterResolver(messageAdapterFactories)
            return Service.Factory(
                ServiceMethodExecutor.Factory(
                    RuntimePlatform.get(),
                    connection,
                    ServiceMethod.Send.Factory(messageAdapterResolver),
                    ServiceMethod.Receive.Factory(dafaultScheduler, connection, textAdapter)
                )
            )
        }
    }
}

