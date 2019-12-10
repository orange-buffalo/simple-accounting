package io.orangebuffalo.simpleaccounting.services.business

import kotlinx.coroutines.CopyableThrowable

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")
class InvalidWorkspaceAccessTokenException(private val token: String) : RuntimeException("Token $token is not valid"),
    CopyableThrowable<InvalidWorkspaceAccessTokenException> {

    // https://github.com/Kotlin/kotlinx.coroutines/issues/1631
    override fun createCopy(): InvalidWorkspaceAccessTokenException? =
        InvalidWorkspaceAccessTokenException(this.token)
}
