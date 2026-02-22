package io.orangebuffalo.simpleaccounting.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.junit.Options
import com.microsoft.playwright.junit.OptionsFactory
import mu.KotlinLogging
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

private val simpleAccountingLogger = KotlinLogging.logger("simple-accounting")

private class SimpleAccountingApp
    : GenericContainer<SimpleAccountingApp>("orangebuffalo/simple-accounting:latest")

private val simpleAccounting: SimpleAccountingApp = SimpleAccountingApp()
    .withEnv("spring_profiles_active", "ci-tests")
    .withLogConsumer { frame -> simpleAccountingLogger.info(frame.utf8String) }
    .waitingFor(Wait.forLogMessage(".*Started SimpleAccountingApplicationKt.*", 1))
    .withExposedPorts(9393)
    .also {
        it.start()
    }

class E2eTestsPlaywrightOptions : OptionsFactory {
    override fun getOptions(): Options = Options()
        .setContextOptions(
            Browser.NewContextOptions()
                .setBaseURL("http://localhost:${simpleAccounting.getMappedPort(9393)}/")
                .setViewportSize(1920, 1080)
        )
        .setHeadless(true)
}
