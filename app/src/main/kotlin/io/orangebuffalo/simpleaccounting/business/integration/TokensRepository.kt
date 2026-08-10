package io.orangebuffalo.simpleaccounting.business.integration

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

private const val tokenLifetimeInMs = 120_000L

@Component
class TokensRepository {

    private val requestsStorage = ConcurrentHashMap<String, StoredRequest>()

    fun storeToken(token: String, request: Any) {
        removeExpiredRequests()
        requestsStorage[token] = StoredRequest(request, System.currentTimeMillis() + tokenLifetimeInMs)
    }

    fun <T : Any> getRequestByToken(token: String, type: kotlin.reflect.KClass<T>): T {
        val storedRequest = requestsStorage[token]
        if (storedRequest == null || storedRequest.expiresAt <= System.currentTimeMillis()) {
            storedRequest?.let { requestsStorage.remove(token, it) }
            throw EntityNotFoundException("Token $token is not found")
        }
        val request = storedRequest.request
        if (!type.isInstance(request)) {
            throw IllegalStateException(
                "Token $token has unexpected type ${request::class.simpleName}, expected ${type.simpleName}"
            )
        }
        @Suppress("UNCHECKED_CAST")
        return request as T
    }

    private fun removeExpiredRequests() {
        val now = System.currentTimeMillis()
        requestsStorage.entries.removeIf { it.value.expiresAt <= now }
    }

    private data class StoredRequest(val request: Any, val expiresAt: Long)
}

inline fun <reified T : Any> TokensRepository.getRequestByToken(token: String): T =
    getRequestByToken(token, T::class)
