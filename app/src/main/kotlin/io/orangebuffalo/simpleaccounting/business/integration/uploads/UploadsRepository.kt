package io.orangebuffalo.simpleaccounting.business.integration.uploads

import io.orangebuffalo.simpleaccounting.business.integration.TokensRepository
import org.springframework.stereotype.Component

@Component
class UploadsRepository(
    private val tokensRepository: TokensRepository,
) {

    suspend fun storeUploadRequest(token: String, workspaceId: Long, userName: String) {
        tokensRepository.storeToken(
            token, PersistentUploadRequest(
                workspaceId = workspaceId,
                userName = userName,
            )
        )
    }

    suspend fun getRequestByToken(token: String): PersistentUploadRequest =
        tokensRepository.getRequestByToken(token) as PersistentUploadRequest
}

data class PersistentUploadRequest(
    val workspaceId: Long,
    val userName: String,
)
