package io.orangebuffalo.simpleaccounting.tests.infra.thirdparty

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import mu.KotlinLogging
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.springframework.test.context.support.TestPropertySourceUtils

private val log = KotlinLogging.logger { }

/**
 * Not expected to be used by tests directly. Intended for higher-level mocks
 * lik [GoogleDriveApiMocks].
 */
object ThirdPartyApisMocks {
    val server: WireMockServer by lazy {
        WireMockServer(
            WireMockConfiguration.options()
                .dynamicPort()
                .notifier(Slf4jNotifier(true))
        ).apply {
            addMockServiceRequestListener { request: Request, _ ->
                log.info { "[WIREMOCK] ${request.method} ${request.url}" }
                log.trace { "[WIREMOCK] Headers: ${request.headers}" }
                log.trace { "[WIREMOCK] Body: ${request.bodyAsString}" }
            }
            start()
            Runtime.getRuntime().addShutdownHook(Thread { stop() })
        }
    }
}

class ThirdPartyApisMocksContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            *GoogleDriveApiMocks.configProperties(),
            *GoogleOAuthMocks.configProperties(),
        )
    }
}

class ThirdPartyApisMocksListener : TestExecutionListener {
    override fun afterTestMethod(testContext: TestContext) {
        ThirdPartyApisMocks.server.resetAll()
        ThirdPartyApisMocks.server.resetMappings()
        ThirdPartyApisMocks.server.resetRequests()
        OAuthMocks.afterTest()
    }

    override fun beforeTestMethod(testContext: TestContext) {
        OAuthMocks.beforeTest(testContext.applicationContext)
    }
}
