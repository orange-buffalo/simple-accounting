package io.orangebuffalo.simpleaccounting.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.assertj.core.api.Assertions.assertThat
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

fun assertNumberOfStubbedRequests(requestsNumber: Int) {
    assertThat(getAllServeEvents()).hasSize(requestsNumber)
}

fun urlEncodeParameter(parameter: Pair<String, String>): String {
    val charset = StandardCharsets.UTF_8.name()
    val encodedKey = URLEncoder.encode(parameter.first, charset)
    val encodedValue = URLEncoder.encode(parameter.second, charset)
    return "$encodedKey=$encodedValue"
}

/**
 * Enables [WireMockServer] for the test. Injects the server port into `wire-mock.port` config property.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(WireMockExtension::class)
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
        removeAllMappings()
        resetAllRequests()
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
