package proxy

import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy

fun main() {
    val annotations = Obj::class.java.getDeclaredField("myField").annotations
    println(annotations[0])
}

internal class Obj {
    @Searchable
    var myField: String? = null
}