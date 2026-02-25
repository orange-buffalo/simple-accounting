package io.orangebuffalo.simpleaccounting.business.security.authentication

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.orangebuffalo.simpleaccounting.business.api.errors.GraphQlBusinessErrorExtensionsProvider
import org.springframework.security.core.AuthenticationException

@GraphQLDescription("Additional error extensions for the ACCOUNT_LOCKED business error.")
data class AccountLockedErrorExtensions(
    @GraphQLDescription("The remaining lock duration in seconds.")
    val lockExpiresInSec: Int
)

class AccountIsTemporaryLockedException(
    lockExpiresInSec: Long
) : AuthenticationException("Account is temporary locked"), GraphQlBusinessErrorExtensionsProvider {
    private val extensions = AccountLockedErrorExtensions(lockExpiresInSec.toInt())
    override fun getBusinessErrorExtensions() = extensions
}
