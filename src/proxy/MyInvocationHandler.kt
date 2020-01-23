package proxy

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class MyInvocationHandler(private val obj: Any) : InvocationHandler {
    @Throws(Throwable::class)
    override fun invoke(
        proxy: Any,
        m: Method,
        args: Array<Any>
    ): Any {
        val result: Any
        result = try {
            if (m.name.contains("get")) {
                println("...get Method Executing...")
            } else {
                println("...set Method Executing...")
            }
            m.invoke(obj, *args)
        } catch (e: InvocationTargetException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
        return result
    }

}