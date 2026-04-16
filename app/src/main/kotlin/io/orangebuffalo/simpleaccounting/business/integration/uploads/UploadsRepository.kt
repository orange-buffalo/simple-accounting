package io.orangebuffalo.simpleaccounting.business.integration.uploads

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component
import jakarta.annotation.PreDestroy

private const val tokenLifetimeInMs = 120_000L

@Component
class UploadsRepository {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val mutex = Mutex()
    private val requestsStorage = mutableMapOf<String, PersistentUploadRequest>()

    @PreDestroy
    fun cancelAsyncJobs() {
        scope.cancel()
    }

    suspend fun storeUploadRequest(token: String, workspaceId: Long, userName: String) {
        mutex.withLock {
            requestsStorage[token] = PersistentUploadRequest(
                workspaceId = workspaceId,
                userName = userName,
            )
        }

        scope.launch {
            delay(tokenLifetimeInMs)
            mutex.withLock {
                requestsStorage.remove(token)
            }
        }
    }

    suspend fun getRequestByToken(token: String): PersistentUploadRequest = mutex.withLock {
        requestsStorage[token] ?: throw EntityNotFoundException("Token $token is not found")
    }
}

data class PersistentUploadRequest(
    val workspaceId: Long,
    val userName: String,
)
