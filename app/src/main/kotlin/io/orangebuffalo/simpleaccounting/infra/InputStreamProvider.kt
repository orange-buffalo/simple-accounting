package io.orangebuffalo.simpleaccounting.infra

import java.io.InputStream

/**
 * Provides scoped access to an input stream. The provider owns and closes the stream;
 * the consumer must fully read it during the callback and must not retain it.
 */
interface InputStreamProvider {
    fun <T> useInputStream(consumer: (InputStream) -> T): T
}

fun inputStreamProvider(streamSupplier: () -> InputStream): InputStreamProvider =
    object : InputStreamProvider {
        override fun <T> useInputStream(consumer: (InputStream) -> T): T = streamSupplier().use(consumer)
    }
