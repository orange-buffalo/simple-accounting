package io.orangebuffalo.simpleaccounting.business.oauthproviders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.time.Duration

private val logger = KotlinLogging.logger {}

private val oidcDiscoveryJson = Json {
    ignoreUnknownKeys = true
}

private const val OIDC_DISCOVERY_PATH = ".well-known/openid-configuration"
private const val MAX_DISCOVERY_DOCUMENT_SIZE_BYTES = 1024 * 1024
private val discoveryRequestTimeout = Duration.ofSeconds(10)

@Service
class OidcProviderDiscoveryService {
    private val restClient = RestClient.builder()
        .requestFactory(
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(discoveryRequestTimeout)
                setReadTimeout(discoveryRequestTimeout)
            }
        )
        .build()

    fun discover(baseUrl: String): OidcProviderConfiguration {
        try {
            val discoveryDocumentUrl = buildDiscoveryDocumentUrl(baseUrl)
            val response = restClient.get()
                .uri(discoveryDocumentUrl)
                .accept(MediaType.APPLICATION_JSON)
                .exchange { _, response ->
                    if (!response.statusCode.is2xxSuccessful) {
                        throw OidcProviderDiscoveryException()
                    }
                    val body = response.body.use {
                        it.readNBytes(MAX_DISCOVERY_DOCUMENT_SIZE_BYTES + 1)
                    }
                    if (body.size > MAX_DISCOVERY_DOCUMENT_SIZE_BYTES) {
                        throw OidcProviderDiscoveryException()
                    }
                    String(body, StandardCharsets.UTF_8)
                }
            val document = oidcDiscoveryJson.decodeFromString<OidcDiscoveryDocument>(response)

            return OidcProviderConfiguration(
                authorizationUrl = document.authorizationEndpoint.requireValue(),
                tokenUrl = document.tokenEndpoint.requireValue(),
                userInfoUrl = document.userInfoEndpoint.requireValue(),
                userIdAttribute = "sub",
                scopes = listOf("openid"),
            )
        } catch (exception: OidcProviderDiscoveryException) {
            throw exception
        } catch (exception: RestClientException) {
            logger.warn(exception) { "Failed to retrieve OIDC configuration from $baseUrl" }
            throw OidcProviderDiscoveryException()
        } catch (exception: SerializationException) {
            logger.warn(exception) { "Failed to parse OIDC configuration from $baseUrl" }
            throw OidcProviderDiscoveryException()
        } catch (exception: IOException) {
            logger.warn(exception) { "Failed to read OIDC configuration from $baseUrl" }
            throw OidcProviderDiscoveryException()
        }
    }

    private fun buildDiscoveryDocumentUrl(baseUrl: String): URI {
        val baseUri = try {
            URI(baseUrl)
        } catch (_: URISyntaxException) {
            throw OidcProviderDiscoveryException()
        }
        if (baseUri.rawQuery != null || baseUri.rawFragment != null) {
            throw OidcProviderDiscoveryException()
        }

        val normalizedBaseUri = URI("${baseUri.toASCIIString().trimEnd('/')}/")
        return normalizedBaseUri.resolve(OIDC_DISCOVERY_PATH)
    }

    private fun String?.requireValue(): String =
        takeUnless { it.isNullOrBlank() } ?: throw OidcProviderDiscoveryException()
}

data class OidcProviderConfiguration(
    val authorizationUrl: String,
    val tokenUrl: String,
    val userInfoUrl: String,
    val userIdAttribute: String,
    val scopes: List<String>,
)

class OidcProviderDiscoveryException : RuntimeException("Failed to discover OIDC provider configuration")

@Serializable
private data class OidcDiscoveryDocument(
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String? = null,
    @SerialName("token_endpoint")
    val tokenEndpoint: String? = null,
    @SerialName("userinfo_endpoint")
    val userInfoEndpoint: String? = null,
)
