package io.orangebuffalo.simpleaccounting.business.integrations.wise

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.net.http.HttpClient
import java.time.Duration
import java.util.Currency

@Component
internal class WiseApiClient(
    properties: WiseProperties,
) {
    private val logger = KotlinLogging.logger {}
    private val client = RestClient.builder().baseUrl(properties.apiBaseUrl)
        .requestFactory(JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build())
            .apply { setReadTimeout(Duration.ofSeconds(20)) })
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    fun getAccounts(token: String): List<WiseAccount> =
        get<List<WiseProfile>>(path = "/2026Q4/profiles", token = token).flatMap { profile ->
            get<List<WiseBalance>>(path = "/2026Q4/profiles/${profile.id}/balances?types=STANDARD", token = token)
                .filter { it.type == "STANDARD" }.map { balance ->
                    WiseAccount(
                        profileId = profile.id, profileName = profile.fullName, profileType = profile.type,
                        accountId = balance.id, currency = balance.currency, name = balance.name, type = balance.type,
                        amountInCents = balanceAmount(balance = balance, profileId = profile.id, token = token),
                    )
                }
        }

    private fun balanceAmount(balance: WiseBalance, profileId: Long, token: String): Long = try {
        balance.amount.value.content.toBigDecimal()
            .movePointRight(Currency.getInstance(balance.currency).defaultFractionDigits).longValueExact()
    } catch (_: IllegalArgumentException) {
        logFailure(endpoint = "/2026Q4/profiles/$profileId/balances?types=STANDARD", token = token, detail = "Invalid balance amount or currency")
        throw WiseUnavailableException()
    } catch (_: ArithmeticException) {
        logFailure(endpoint = "/2026Q4/profiles/$profileId/balances?types=STANDARD", token = token, detail = "Unrepresentable balance amount")
        throw WiseUnavailableException()
    }

    private inline fun <reified T> get(path: String, token: String): T {
        var body: String? = null
        var status: Int? = null
        var endpoint = path
        try {
            val response = client.get().uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .httpRequest { request -> endpoint = request.uri.toString() }
                .retrieve().toEntity(String::class.java)
            status = response.statusCode.value()
            body = response.body
            if (body.isNullOrBlank()) {
                logFailure(endpoint, token, "HTTP $status; empty response body")
                throw WiseUnavailableException()
            }
            return json.decodeFromString<T>(body)
        } catch (e: RestClientResponseException) {
            logFailure(endpoint, token, "HTTP ${e.statusCode.value()}; response body: ${e.responseBodyAsString.ifBlank { "<empty>" }}")
            if (e.statusCode.value() in listOf(401, 403)) throw WiseInvalidTokenException()
            throw WiseUnavailableException()
        } catch (e: RestClientException) {
            val causes = generateSequence<Throwable>(e) { it.cause }.take(8)
                .joinToString("; ") { "${it.javaClass.simpleName}: ${it.message}" }
            logFailure(endpoint, token, "No usable response received; $causes")
            throw WiseUnavailableException()
        } catch (_: SerializationException) {
            logFailure(endpoint, token, "HTTP $status; invalid response body: $body")
            throw WiseUnavailableException()
        }
    }

    private fun logFailure(endpoint: String, token: String, detail: String) {
        val diagnostic = "Wise GET $endpoint failed: $detail"
            .replace(token, "[REDACTED]")
            .replace(Regex("[\\r\\n\\t]"), " ")
        logger.warn { diagnostic.take(4096) }
    }
}

@Serializable
private data class WiseProfile(val id: Long, val fullName: String, val type: String)

@Serializable
private data class WiseBalance(val id: Long, val currency: String, val name: String? = null, val type: String, val amount: WiseAmount)

@Serializable
private data class WiseAmount(val value: JsonPrimitive)
