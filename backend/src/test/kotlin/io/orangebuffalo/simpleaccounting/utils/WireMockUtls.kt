package io.orangebuffalo.simpleaccounting.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.extension.*
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory
import org.springframework.test.context.MergedContextConfiguration
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun assertNumberOfReceivedWireMockRequests(requestsNumber: Int) {
    assertThat(getAllServeEvents()).hasSize(requestsNumber)
}

fun urlEncodeParameter(parameter: Pair<String, String>): String {
    val charset = StandardCharsets.UTF_8.name()
    val encodedKey = URLEncoder.encode(parameter.first, charset)
    val encodedValue = URLEncoder.encode(parameter.second, charset)
    return "$encodedKey=$encodedValue"
}

fun stubGetRequestTo(path: String, spec: MappingBuilder.() -> Unit) {
    val mappingBuilder = get(urlPathEqualTo(path))
    spec(mappingBuilder)
    stubFor(mappingBuilder)
}

fun stubPostRequestTo(path: String, spec: MappingBuilder.() -> Unit) {
    val mappingBuilder = post(urlPathEqualTo(path))
    spec(mappingBuilder)
    stubFor(mappingBuilder)
}

fun MappingBuilder.willReturnOkJson(jsonBody: String): MappingBuilder = willReturn(okJson(jsonBody))

fun MappingBuilder.willReturnResponse(spec: ResponseDefinitionBuilder.() -> Unit) {
    val responseDefinitionBuilder = aResponse()
    spec(responseDefinitionBuilder)
    willReturn(responseDefinitionBuilder)
}

/**
 * Enables [WireMockServer] for the test. Injects the server port into `wire-mock.port` config property.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(WireMockExtension::class)
// TODO #468: enable once wiremock supports Jetty 11
@Disabled("Disabled as Wiremock does not support Jetty 11 yet")
annotation class NeedsWireMock

/**
 * Injects WireMock server port into a constructor, test or test lifecycle method.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(WireMockExtension::class)
annotation class WireMockPort

private val wireMockServer: WireMockServer by lazy {
    WireMockServer(options().dynamicPort()).apply { start() }
}

private fun getWireMockPort(): Int {
    if (!wireMockServer.isRunning) {
        wireMockServer.start()
    }
    return wireMockServer.port()
}

/**
 * JUnit extensions to configure the [WireMockServer] and handle its lifecycle.
 */
class WireMockExtension : Extension, BeforeAllCallback, AfterAllCallback, AfterEachCallback, ParameterResolver {
    override fun afterAll(context: ExtensionContext?) {
        wireMockServer.stop()
    }

    override fun beforeAll(context: ExtensionContext?) {
        configureFor(getWireMockPort())
    }

    override fun afterEach(context: ExtensionContext?) {
        try {
            assertThat(findUnmatchedRequests()).isEmpty()
        } finally {
            removeAllMappings()
            resetAllRequests()
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext?) =
        parameterContext.isAnnotated(WireMockPort::class.java)

    override fun resolveParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?) =
        getWireMockPort()
}

/**
 * Customizes Spring Context to inject WireMock port into the config properties.
 */
class WireMockContextCustomizerFactory : ContextCustomizerFactory {
    override fun createContextCustomizer(
        testClass: Class<*>, configAttributes: MutableList<ContextConfigurationAttributes>
    ): ContextCustomizer? =
        if (MergedAnnotations.from(testClass).isPresent(NeedsWireMock::class.java)) {
            WireMockContextCustomizer()
        } else {
            null
        }
}

private class WireMockContextCustomizer : ContextCustomizer {
    override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {
        val sources = context.environment.propertySources
        sources.addLast(MapPropertySource("wireMockProperties", mapOf("wire-mock.port" to getWireMockPort())))
    }
}
