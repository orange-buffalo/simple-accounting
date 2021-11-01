package io.orangebuffalo.simpleaccounting.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.security.util.InMemoryResource
import org.springframework.util.StreamUtils
import java.io.ByteArrayOutputStream

/**
 * Reads and releases the flux of data buffers and converts the bytes sequence into a string.
 */
fun Flow<DataBuffer>.consumeToString(): String {
    val os = ByteArrayOutputStream()
    DataBufferUtils.write(this.asFlux(), os)
        .map { DataBufferUtils.release(it) }
        .blockLast()
    return String(os.toByteArray())
}

/**
 * Creates data buffers publisher from the string bytes.
 */
fun String.toDataBuffers(): Flow<DataBuffer> =
    DataBufferUtils.read(
        InMemoryResource(this),
        DefaultDataBufferFactory.sharedInstance,
        StreamUtils.BUFFER_SIZE
    ).asFlow()
