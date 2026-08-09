package io.orangebuffalo.simpleaccounting.business.integration.downloads

import io.orangebuffalo.simpleaccounting.infra.InputStreamProvider

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
    fun getContent(metadata: T): DownloadContentResponse
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
     * Scoped provider of the content stream.
     */
    val content: InputStreamProvider,

    val contentType: String
)
