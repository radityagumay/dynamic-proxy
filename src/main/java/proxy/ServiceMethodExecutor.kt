package proxy

import java.lang.reflect.Method

class ServiceMethodExecutor(
    private val methods: Map<Method, ServiceMethod>
) {
    fun execute(method: Method, args: Array<Any>?): Any {
        val serviceMethod = checkNotNull(methods[method]) { "Service method not found" }
        return when (serviceMethod) {
            is ServiceMethod.Send -> serviceMethod.execute(args!![0])
            is ServiceMethod.Receive -> serviceMethod.execute()
        }
    }

    class Factory(
        private val runtimePlatform: RuntimePlatform,
        private val connection: Connection,
        private val sendServiceMethodFactory: ServiceMethod.Send.Factory,
        private val receiveServiceMethodFactory: ServiceMethod.Receive.Factory
    ) {
        fun create(serviceInterface: Class<*>): ServiceMethodExecutor {
            val serviceMethods = serviceInterface.findServiceMethods()
            return ServiceMethodExecutor(serviceMethods)
        }

        private fun Class<*>.findServiceMethods(): Map<Method, ServiceMethod> {
            val methods = declaredMethods.filterNot { runtimePlatform.isDefaultMethod(it) }
            val serviceMethods = methods.map { it.toServiceMethod() }
            return methods.zip(serviceMethods).toMap()
        }

        private fun Method.toServiceMethod(): ServiceMethod {
            val serviceMethodFactories = annotations.mapNotNull { it.findServiceMethodFactory() }
            require(serviceMethodFactories.size == 1) {
                "A method must have one and only one service method annotation: $this"
            }
            return serviceMethodFactories.first().create(connection, this)
        }

        private fun Annotation.findServiceMethodFactory(): ServiceMethod.Factory? {
            return when (this) {
                is Send -> sendServiceMethodFactory
                is Receive -> receiveServiceMethodFactory
                else -> null
            }
        }
    }
}