package proxy

import proxy.adapter.TextMessageAdapter
import proxy.adapter.factory.BuiltInMessageAdapterFactory
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
        fun build() = Scarlet(createServiceFactory())

        private fun createServiceFactory(): Service.Factory {
            val connection = Connection()
            val textAdapter = TextMessageAdapter()
            val messageFactory = BuiltInMessageAdapterFactory(textAdapter)
            val messageAdapter = MessageAdapterResolver(listOf(messageFactory))
            return Service.Factory(
                ServiceMethodExecutor.Factory(
                    RuntimePlatform.get(),
                    connection,
                    ServiceMethod.Send.Factory(messageAdapter),
                    ServiceMethod.Receive.Factory(connection, textAdapter)
                )
            )
        }
    }
}

