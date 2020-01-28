package proxy

import io.reactivex.exceptions.CompositeException
import proxy.adapter.StreamAdapter
import java.lang.reflect.Type

class StreamAdapterResolver(
    private val streamAdapterFactories: List<StreamAdapter.Factory>
) {
    fun resolve(type: Type): StreamAdapter<Any, Any> {
        val throwables = mutableListOf<Throwable>()
        for (adapterFactory in streamAdapterFactories) {
            try {
                return adapterFactory.create(type)
            } catch (e: Throwable) {
                // This type is not supported by this adapter
                throwables.add(e)
            }
        }
        val compositeException = CompositeException(*throwables.toTypedArray())
        throw IllegalStateException("Cannot resolve stream adapter for type $type.", compositeException)
    }
}
