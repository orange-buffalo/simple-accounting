package io.orangebuffalo.simpleaccounting.infra.security

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestExecutionListener

/**
 * Replaces [org.springframework.security.crypto.password.PasswordEncoder] with mock implementation
 * to speedup login process and control it easily in the tests.
 *
 * Works in conjunction with [TestPasswordEncoderListener].
 */
@Configuration
class TestPasswordEncoderConfig {

    @Suppress("unused") // creates a mock
    @MockBean
    lateinit var passwordEncoder: PasswordEncoder
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
