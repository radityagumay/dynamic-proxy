package proxy

import java.lang.reflect.Proxy

class ProxyFactory {

    fun <T> create(service: Class<T>) = implementation(service)

    inline fun <reified T : Any> create(): T = create(T::class.java)

    private fun <T> implementation(service: Class<T>): T {
        val proxy = Proxy.newProxyInstance(
            service.classLoader,
            arrayOf(service),
            MyInvocationHandler(TaskImpl())
        )
        return service.cast(proxy)
    }
}
