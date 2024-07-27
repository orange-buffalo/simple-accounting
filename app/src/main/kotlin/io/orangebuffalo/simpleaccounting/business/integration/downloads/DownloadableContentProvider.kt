package io.orangebuffalo.simpleaccounting.business.integration.downloads

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer

/**
 * Service capable to provide the downloadable content.
 *
 * @param T type of the metadata required by this provider to lookup and provide the content.
 * @see DownloadsService
 */
interface DownloadableContentProvider<T : Any> {

    /**
     * Unique identifier of this provider.
     */
    fun getId(): String

    /**
     * Provides the content by metadata.
     */
    suspend fun getContent(metadata: T): DownloadContentResponse
}

data class DownloadContentResponse(

    /**
     * File name for this content.
     */
    val fileName: String,

    /**
     * If known, the size of the content. `null` otherwise.
     */
    val sizeInBytes: Long?,

    /**
     * Cold publisher of content data. Caller is responsible for releasing the buffers.
     */
    val content: Flow<DataBuffer>,

    // todo #108
    val contentType: String? = null
)

