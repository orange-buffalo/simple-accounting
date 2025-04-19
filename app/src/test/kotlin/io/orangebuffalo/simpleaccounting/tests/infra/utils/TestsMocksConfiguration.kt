package io.orangebuffalo.simpleaccounting.tests.infra.utils

import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.infra.TimeServiceImpl
import io.orangebuffalo.simpleaccounting.infra.TokenGenerator
import io.orangebuffalo.simpleaccounting.infra.TokenGeneratorImpl
import io.orangebuffalo.simpleaccounting.infra.thirdparty.JbrWorkaround
import org.mockito.kotlin.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

/**
 * Setups common mocks for the tests to avoid creation of extra Spring Contexts
 * and so speedup the tests.
 *
 * See Spring limitation described in https://github.com/spring-projects/spring-framework/issues/33934#issuecomment-2512136161.
 * Waiting for https://github.com/spring-projects/spring-framework/issues/33925 to be available - we then
 * can convert this workaround with primary bean into a meta-annotation.
 */
@TestConfiguration
class TestsMocksConfiguration {

    @Bean
    @Primary
    fun testTimeService(): TimeService = mock { }

    @Bean
    @Primary
    fun testPasswordEncoder(): PasswordEncoder = mock { }

    @Bean
    @Primary
    fun testTokenGenerator(): TokenGenerator = mock { }
}

/**
 * Replaces [org.springframework.security.crypto.password.PasswordEncoder] with mock implementation
 * to speedup login process and control it easily in the tests.
 *
 * Works in conjunction with [TestsMocksConfiguration].
 */
class TestsMocksListener : TestExecutionListener {
    override fun beforeTestMethod(testContext: TestContext) {
        mockPasswordEncoder(testContext.applicationContext)
        mockTimeService(testContext.applicationContext)
        mockTokenGenerator(testContext.applicationContext)
    }

    @JbrWorkaround
    private fun mockTokenGenerator(applicationContext: ApplicationContext) {
        val original = TokenGeneratorImpl()
        val mock = applicationContext.getBean(TokenGenerator::class.java)
        reset(mock)
        whenever(mock.generateToken(any())) doAnswer { original.generateToken(it.arguments[0] as Int) }
        whenever(mock.generateUuid()) doAnswer { original.generateUuid() }
    }

    @JbrWorkaround
    private fun mockTimeService(applicationContext: ApplicationContext) {
        val original = TimeServiceImpl()
        val mock = applicationContext.getBean(TimeService::class.java)
        reset(mock)
        whenever(mock.currentTime()) doAnswer { original.currentTime() }
        whenever(mock.currentDate()) doAnswer { original.currentDate() }
    }

    private fun mockPasswordEncoder(applicationContext: ApplicationContext) {
        val passwordEncoder = applicationContext.getBean(PasswordEncoder::class.java)
        reset(passwordEncoder)
        whenever(passwordEncoder.matches(any(), any())) doReturn true
        whenever(passwordEncoder.encode(any())) doAnswer { it.arguments[0] as String }
    }
}
