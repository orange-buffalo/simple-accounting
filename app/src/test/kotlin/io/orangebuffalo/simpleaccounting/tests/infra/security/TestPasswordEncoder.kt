package io.orangebuffalo.simpleaccounting.tests.infra.security

import com.nhaarman.mockitokotlin2.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestExecutionListener

/**
 * Replaces [org.springframework.security.crypto.password.PasswordEncoder] with mock implementation
 * to speedup login process and control it easily in the tests.
 *
 * Works in conjunction with [TestPasswordEncoderListener].
 */
@TestConfiguration
class TestPasswordEncoderConfig {

    /**
     * See Spring limitation described in https://github.com/spring-projects/spring-framework/issues/33934#issuecomment-2512136161.
     * Waiting for https://github.com/spring-projects/spring-framework/issues/33925 to be available - we then
     * can convert this workaround with primary bean into a meta-annotation.
     */
    @Bean
    @Primary
    fun testPasswordEncoder(): PasswordEncoder = mock { }
}

/**
 * Replaces [org.springframework.security.crypto.password.PasswordEncoder] with mock implementation
 * to speedup login process and control it easily in the tests.
 *
 * Works in conjunction with [TestPasswordEncoderConfig].
 */
class TestPasswordEncoderListener : TestExecutionListener {
    override fun beforeTestMethod(testContext: org.springframework.test.context.TestContext) {
        val passwordEncoder = testContext.applicationContext.getBean(PasswordEncoder::class.java)
        whenever(passwordEncoder.matches(any(), any())) doReturn true
        whenever(passwordEncoder.encode(any())) doAnswer { it.arguments[0] as String }
    }
}
