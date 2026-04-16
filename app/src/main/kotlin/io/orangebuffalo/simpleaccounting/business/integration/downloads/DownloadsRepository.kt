package io.orangebuffalo.simpleaccounting.business.integration.downloads

import io.orangebuffalo.simpleaccounting.business.integration.TokensRepository
import org.springframework.stereotype.Component

@Component
class DownloadsRepository(
    private val tokensRepository: TokensRepository,
) {

    suspend fun storeDownloadRequest(token: String, providerId: String, metadata: Any, userName: String) {
        tokensRepository.storeToken(
            token, PersistentDownloadRequest(
                providerId = providerId,
                metadata = metadata,
                userName = userName
            )
        )
    }

    suspend fun getRequestByToken(token: String): PersistentDownloadRequest =
        tokensRepository.getRequestByToken(token) as PersistentDownloadRequest
}

data class PersistentDownloadRequest(
    val providerId: String,
    val metadata: Any,
    val userName: String
)
