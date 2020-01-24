package proxy

import java.lang.reflect.*


fun main() {
    val scarlet = Scarlet.Builder()
        .build()
    val service = scarlet.create<Task>()
    service.sendText("hello world")
    service.sendText("hello world")
    service.sendText("hello world")
    service.sendText("hello world")
    service.sendText("hello world")
    service.sendText("hello world")

    service.observeEvents().forEach {
        println(it)
    }
}

interface Task {
    @Send
    fun sendText(message: String)

    @Receive
    fun observeEvents(): List<String>
}

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
                connection,
                ServiceMethodExecutor.Factory(
                    connection,
                    ServiceMethod.Send.Factory(messageAdapter),
                    ServiceMethod.Receive.Factory(connection, textAdapter)
                )
            )
        }
    }
}

class BuiltInMessageAdapterFactory(
    private val textAdapter: TextMessageAdapter
) : MessageAdapter.Factory {
    override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> = when (type.getRawType()) {
        String::class.java -> textAdapter
        else -> throw IllegalArgumentException("Type is not supported by this MessageAdapterFactory: $type")
    }
}

class TextMessageAdapter : MessageAdapter<String> {
    override fun fromMessage(message: Message): String = when (message) {
        is Message.Text -> message.value
        else -> throw IllegalArgumentException("This Message Adapter only supports text Messages")
    }

    override fun toMessage(data: String): Message = Message.Text(data)
}


class Service(
    private val connection: Connection,
    private val executor: ServiceMethodExecutor
) {
    fun execute(method: Method, args: Array<Any>?) = executor.execute(method, args)

    class Factory(
        private val connection: Connection,
        private val executorFactory: ServiceMethodExecutor.Factory
    ) {
        fun create(serviceInterface: Class<*>): Service {
            validateService(serviceInterface)
            val serviceMethodExecutor = executorFactory.create(serviceInterface)
            return Service(connection, serviceMethodExecutor)
        }

        private fun validateService(service: Class<*>) {
            require(service.isInterface) { "Service declarations must be interfaces." }
        }
    }
}

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
        private val connection: Connection,
        private val sendServiceMethodFactory: ServiceMethod.Send.Factory,
        private val receiveServiceMethodFactory: ServiceMethod.Receive.Factory

    ) {
        fun create(serviceInterface: Class<*>): ServiceMethodExecutor {
            val serviceMethods = serviceInterface.findServiceMethods()
            return ServiceMethodExecutor(serviceMethods)
        }

        private fun Class<*>.findServiceMethods(): Map<Method, ServiceMethod> {
            val methods = declaredMethods
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

        private fun Annotation.findServiceMethodFactory(): ServiceMethod.Factory? =
            when (this) {
                is Send -> sendServiceMethodFactory
                is Receive -> receiveServiceMethodFactory
                else -> receiveServiceMethodFactory
            }
    }
}

sealed class ServiceMethod {
    interface Factory {
        fun create(connection: Connection, method: Method): ServiceMethod
    }

    class Send(
        private val connection: Connection,
        private val messageAdapter: MessageAdapter<Any>
    ) : ServiceMethod() {

        fun execute(data: Any): Any {
            val message = messageAdapter.toMessage(data)
            return connection.send(message)
        }

        class Factory(
            private val messageAdapterResolver: MessageAdapterResolver
        ) : ServiceMethod.Factory {
            override fun create(connection: Connection, method: Method): ServiceMethod {
                method.requireParameterTypes(Any::class.java) {
                    "Send method must have one and only one parameter: $method"
                }
                method.requireReturnTypeIsOneOf(Boolean::class.java, Void.TYPE) {
                    "Send method must return Boolean or Void: $method"
                }

                val messageType = method.getFirstParameterType()
                val annotations = method.getFirstParameterAnnotations()
                val adapter = messageAdapterResolver.resolve(messageType, annotations)
                return Send(connection, adapter)
            }
        }
    }

    class Receive(
        private val connection: Connection,
        private val messageFactory: TextMessageAdapter
    ) : ServiceMethod() {
        fun execute(): Any {
            return connection.observeEvents().map { messageFactory.fromMessage(it) }
        }

        class Factory(
            private val connection: Connection,
            private val textMessageAdapter: TextMessageAdapter
        ) : ServiceMethod.Factory {
            override fun create(connection: Connection, method: Method): ServiceMethod {
                method.requireParameterTypes { "Receive method must have zero parameter: $method" }
                method.requireReturnTypeIsOneOf(ParameterizedType::class.java) {
                    "Receive method must return ParameterizedType: $method"
                }
                method.requireReturnTypeIsResolvable {
                    "Method return type must not include a type variable or wildcard: ${method.genericReturnType}"
                }
                return Receive(connection, textMessageAdapter)
            }
        }
    }

    companion object {
        private inline fun Method.requireParameterTypes(vararg types: Class<*>, lazyMessage: () -> Any) {
            require(genericParameterTypes.size == types.size, lazyMessage)
            require(genericParameterTypes.zip(types).all { (t1, t2) -> t2 === t1 || t2.isInstance(t1) }, lazyMessage)
        }

        private inline fun Method.requireReturnTypeIsOneOf(vararg types: Class<*>, lazyMessage: () -> Any) =
            require(types.any { it === genericReturnType || it.isInstance(genericReturnType) }, lazyMessage)

        private inline fun Method.requireReturnTypeIsResolvable(lazyMessage: () -> Any) =
            require(!genericReturnType.hasUnresolvableType(), lazyMessage)

        private fun Method.getFirstParameterType(): Type = genericParameterTypes.first()

        private fun Method.getFirstParameterAnnotations(): Array<Annotation> = parameterAnnotations.first()
    }
}


fun Type.getRawType(): Class<*> = Utils.getRawType(this)

fun Type.hasUnresolvableType(): Boolean = Utils.hasUnresolvableType(this)

fun ParameterizedType.getParameterUpperBound(index: Int): Type =
    Utils.getParameterUpperBound(index, this)

