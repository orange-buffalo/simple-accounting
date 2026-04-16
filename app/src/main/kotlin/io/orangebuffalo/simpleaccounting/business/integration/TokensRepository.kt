package io.orangebuffalo.simpleaccounting.business.integration

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component
import jakarta.annotation.PreDestroy

private const val tokenLifetimeInMs = 120_000L

@Component
class TokensRepository {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val mutex = Mutex()
    private val requestsStorage = mutableMapOf<String, Any>()

    @PreDestroy
    fun cancelAsyncJobs() {
        scope.cancel()
    }

    suspend fun storeToken(token: String, request: Any) {
        mutex.withLock {
            requestsStorage[token] = request
        }

        scope.launch {
            delay(tokenLifetimeInMs)
            mutex.withLock {
                requestsStorage.remove(token)
            }
        }
    }

    suspend fun getRequestByToken(token: String): Any = mutex.withLock {
        requestsStorage[token] ?: throw EntityNotFoundException("Token $token is not found")
    }
}
