package proxy

import io.reactivex.Scheduler
import proxy.adapter.MessageAdapter
import proxy.adapter.TextMessageAdapter
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

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
        private val scheduler: Scheduler,
        private val connection: Connection,
        private val messageAdapter: TextMessageAdapter
    ) : ServiceMethod() {
        fun execute(): Any {
            return connection.observeEvents()
                .observeOn(scheduler)
                .map {
                    messageAdapter.fromMessage(it)
                }
        }

        class Factory(
            private val scheduler: Scheduler,
            private val conn: Connection,
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
                return Receive(scheduler, conn, textMessageAdapter)
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