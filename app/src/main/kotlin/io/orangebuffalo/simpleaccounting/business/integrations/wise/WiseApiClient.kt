package io.orangebuffalo.simpleaccounting.business.integrations.wise

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.springframework.http.HttpHeaders
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.net.http.HttpClient
import java.time.Duration

@Component
internal class WiseApiClient(
    properties: WiseProperties,
) {
    private val client = RestClient.builder().baseUrl(properties.apiBaseUrl)
        .requestFactory(JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build())
            .apply { setReadTimeout(Duration.ofSeconds(20)) })
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    fun getAccounts(token: String): List<WiseAccount> =
        get<List<WiseProfile>>("/2026Q4/profiles", token).flatMap { profile ->
            get<List<WiseBalance>>("/2026Q4/profiles/${profile.id}/balances?types=STANDARD,SAVINGS", token).map { balance ->
                WiseAccount(profile.id, profile.fullName, balance.id, balance.currency, balance.name, balance.type)
            }
        }

    private inline fun <reified T> get(path: String, token: String): T {
        try {
            val body = client.get().uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve().body(String::class.java) ?: throw WiseUnavailableException()
            return json.decodeFromString<T>(body)
        } catch (_: HttpClientErrorException.Unauthorized) {
            throw WiseInvalidTokenException()
        } catch (_: HttpClientErrorException.Forbidden) {
            throw WiseInvalidTokenException()
        } catch (_: RestClientException) {
            throw WiseUnavailableException()
        } catch (_: SerializationException) {
            throw WiseUnavailableException()
        }
    }
}

@Serializable
private data class WiseProfile(val id: Long, val fullName: String)

@Serializable
private data class WiseBalance(val id: Long, val currency: String, val name: String? = null, val type: String)
