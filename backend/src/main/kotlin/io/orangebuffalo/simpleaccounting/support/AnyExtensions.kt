package io.orangebuffalo.simpleaccounting.support

import java.util.*

inline fun <reified T : Any> T.kotlinEquals(
    other: Any?,
    properties: Array<out (obj: T) -> Any?>,
    noinline superEquals: (() -> Boolean)? = null
): Boolean {
    return when {
        other === this -> true
        other !is T -> false
        superEquals != null && !superEquals() -> false
        else -> properties.all {
            val property = it(this)
            val otherProperty = it(other)
            if (property is Array<*>) {
                Objects.deepEquals(property, otherProperty)
            } else {
                Objects.equals(property, otherProperty)
            }
        }
    }
}

inline fun <reified T : Any> T.kotlinHashCode(
    properties: Array<out (obj: T) -> Any?>,
    noinline superHashCode: (() -> Int)? = null
): Int {
    val values = Array(properties.size) { i ->
        val property = properties[i](this)
        if (property is Array<*>) {
            property.contentDeepHashCode()
        } else {
            property
        }
    }

    return if (superHashCode != null) {
        Objects.hash(*values, superHashCode())
    } else {
        Objects.hash(*values)
    }
}
