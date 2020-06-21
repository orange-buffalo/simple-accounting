package io.orangebuffalo.simpleaccounting.services.integration.downloads

import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component

private const val tokenLifetimeInMs = 120_000L

@Component
class DownloadsRepository {

    private val mutex = Mutex()
    private val requestsStorage = mutableMapOf<String, PersistentDownloadRequest>()

    suspend fun storeDownloadRequest(token: String, providerId: String, metadata: Any, userName: String) {
        mutex.withLock {
            requestsStorage[token] = PersistentDownloadRequest(
                providerId = providerId,
                metadata = metadata,
                userName = userName
            )
        }

        GlobalScope.launch {
            delay(tokenLifetimeInMs)
            mutex.withLock {
                requestsStorage.remove(token)
            }
        }

    }

    suspend fun getRequestByToken(token: String): PersistentDownloadRequest =
        requestsStorage[token] ?: throw EntityNotFoundException("Token $token is not found")

}

data class PersistentDownloadRequest(
    val providerId: String,
    val metadata: Any,
    val userName: String
)
