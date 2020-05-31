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

fun assertNumberOfStubbedRequests(requestsNumber: Int) {
    assertThat(getAllServeEvents()).hasSize(requestsNumber)
}

/**
 * Enables [WireMockServer] for the test. Injects the server port into `wire-mock.port` config property.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(WireMockExtension::class)
annotation class NeedsWireMock

private val wireMockServer: WireMockServer by lazy {
    WireMockServer(options().dynamicPort()).apply { start() }
}

/**
 * JUnit extensions to configure the [WireMockServer] and handle its lifecycle.
 */
class WireMockExtension : Extension, BeforeAllCallback, AfterAllCallback, AfterEachCallback {
    override fun afterAll(context: ExtensionContext?) {
        wireMockServer.stop()
    }

    override fun beforeAll(context: ExtensionContext?) {
        configureFor(wireMockServer.port())
    }

    override fun afterEach(context: ExtensionContext?) {
        removeAllMappings()
        resetAllRequests()
    }
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
        sources.addLast(MapPropertySource("wireMockProperties", mapOf("wire-mock.port" to wireMockServer.port())))
    }
}
