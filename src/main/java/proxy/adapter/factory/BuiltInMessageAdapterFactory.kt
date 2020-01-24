package proxy.adapter.factory

import proxy.adapter.MessageAdapter
import proxy.adapter.TextMessageAdapter
import proxy.getRawType
import java.lang.reflect.Type

class BuiltInMessageAdapterFactory(
    private val textAdapter: TextMessageAdapter
) : MessageAdapter.Factory {
    override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> = when (type.getRawType()) {
        String::class.java -> textAdapter
        else -> throw IllegalArgumentException("Type is not supported by this MessageAdapterFactory: $type")
    }
}