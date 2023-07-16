package io.orangebuffalo.simpleaccounting.infra.ui

import org.springframework.security.crypto.password.PasswordEncoder

class TestPasswordEncoder : PasswordEncoder {

    var replyWithSuccess: Boolean = true

    override fun encode(rawPassword: CharSequence): String = rawPassword.toString()

    override fun matches(rawPassword: CharSequence, encodedPassword: String): Boolean = replyWithSuccess
}
