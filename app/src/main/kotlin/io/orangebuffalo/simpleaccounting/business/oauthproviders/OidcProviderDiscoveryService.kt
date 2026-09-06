package io.orangebuffalo.simpleaccounting.business.oauthproviders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

private val logger = KotlinLogging.logger {}

private val oidcDiscoveryJson = Json {
    ignoreUnknownKeys = true
}

@Service
class OidcProviderDiscoveryService {
    private val restClient = RestClient.create()

    fun discover(baseUrl: String): OidcProviderConfiguration {
        try {
            val discoveryDocumentUrl = "${baseUrl.trimEnd('/')}/.well-known/openid-configuration"
            val response = restClient.get()
                .uri(discoveryDocumentUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String::class.java)
                ?: throw OidcProviderDiscoveryException()
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
        }
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
