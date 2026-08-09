package io.orangebuffalo.simpleaccounting.infra

import java.io.InputStream

/**
 * Provides scoped access to an input stream. The provider owns and closes the stream;
 * the consumer must fully read it during the callback and must not retain it.
 */
fun interface InputStreamProvider {
    fun useInputStream(consumer: (InputStream) -> Unit)
}

fun inputStreamProvider(streamSupplier: () -> InputStream): InputStreamProvider =
    InputStreamProvider { consumer -> streamSupplier().use(consumer) }
