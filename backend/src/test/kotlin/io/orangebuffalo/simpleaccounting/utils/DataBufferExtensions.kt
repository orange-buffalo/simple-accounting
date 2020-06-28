package io.orangebuffalo.simpleaccounting.utils

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.security.util.InMemoryResource
import org.springframework.util.StreamUtils
import reactor.core.publisher.Flux
import java.io.ByteArrayOutputStream

private val dataBufferFactory = DefaultDataBufferFactory()

/**
 * Reads and releases the flux of data buffers and converts the bytes sequence into a string.
 */
fun Flux<DataBuffer>.consumeToString(): String {
    val os = ByteArrayOutputStream()
    DataBufferUtils.write(this, os)
        .map { DataBufferUtils.release(it) }
        .blockLast()
    return String(os.toByteArray())
}

/**
 * Creates data buffers publisher from the string bytes.
 */
fun String.toDataBuffers(): Flux<DataBuffer> =
    DataBufferUtils.read(
        InMemoryResource(this),
        dataBufferFactory,
        StreamUtils.BUFFER_SIZE
    )
