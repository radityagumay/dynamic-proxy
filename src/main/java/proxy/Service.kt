package proxy

import java.lang.reflect.Method

class Service(
    private val executor: ServiceMethodExecutor
) {
    fun execute(method: Method, args: Array<Any>?) = executor.execute(method, args)

    class Factory(
        private val executorFactory: ServiceMethodExecutor.Factory
    ) {
        fun create(serviceInterface: Class<*>): Service {
            validateService(serviceInterface)
            val serviceMethodExecutor = executorFactory.create(serviceInterface)
            return Service(serviceMethodExecutor)
        }

        private fun validateService(service: Class<*>) {
            require(service.isInterface) { "Service declarations must be interfaces." }
        }
    }
}