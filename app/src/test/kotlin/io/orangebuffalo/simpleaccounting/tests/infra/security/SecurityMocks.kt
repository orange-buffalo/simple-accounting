package io.orangebuffalo.simpleaccounting.tests.infra.security

import io.orangebuffalo.simpleaccounting.business.security.createRegularUserPrincipal
import io.orangebuffalo.simpleaccounting.business.security.createTransientUserPrincipal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import org.springframework.test.web.reactive.server.WebTestClient

@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = SaMockSecurityContextFactory::class)
@Deprecated("Use ApiTestClient for actual JWT auth")
annotation class WithSaMockUser(
    val userName: String = "",
    val roles: Array<String> = [],
    val transient: Boolean = false,
    val workspaceAccessToken: String = ""
)

@Retention(AnnotationRetention.RUNTIME)
@WithSaMockUser(roles = ["USER"], userName = "Fry")
@Deprecated("Use ApiTestClient for actual JWT auth")
annotation class WithMockFryUser

@Retention(AnnotationRetention.RUNTIME)
@WithSaMockUser(roles = ["USER"], userName = "Zoidberg")
@Deprecated("Use ApiTestClient for actual JWT auth")
annotation class WithMockZoidbergUser

@Retention(AnnotationRetention.RUNTIME)
@WithSaMockUser(roles = ["ADMIN"], userName = "Farnsworth")
@Deprecated("Use ApiTestClient for actual JWT auth")
annotation class WithMockFarnsworthUser

@Retention(AnnotationRetention.RUNTIME)
@WithSaMockUser(roles = ["USER"], userName = "Roberto")
@Deprecated("Use ApiTestClient for actual JWT auth")
annotation class WithMockRobertoUser

@Retention(AnnotationRetention.RUNTIME)
@WithSaMockUser(roles = ["USER"], userName = "MafiaBot")
@Deprecated("Use ApiTestClient for actual JWT auth")
annotation class WithMockMafiaBotUser

@Deprecated("Use ApiTestClient for actual JWT auth")
class SaMockSecurityContextFactory : WithSecurityContextFactory<WithSaMockUser> {
    override fun createSecurityContext(withSaMockUser: WithSaMockUser): SecurityContext {
        val context = SecurityContextHolder.createEmptyContext()
        val principal = if (withSaMockUser.transient) {
            createTransientUserPrincipal(withSaMockUser.workspaceAccessToken)
        } else {
            createRegularUserPrincipal(withSaMockUser.userName, "", withSaMockUser.roles.asList())
        }
        context.authentication = UsernamePasswordAuthenticationToken(principal, "", principal.authorities)
        return context
    }
}

@Deprecated("Use ApiTestClient for actual JWT auth")
fun WebTestClient.asFarnsworth(): WebTestClient {
    val principal = createRegularUserPrincipal("Farnsworth", "", listOf("ADMIN"))
    val authentication = UsernamePasswordAuthenticationToken(principal, "", principal.authorities)
    return this.mutateWith(mockAuthentication(authentication))
}

@Deprecated("Use ApiTestClient for actual JWT auth")
fun WebTestClient.asFry(): WebTestClient {
    val principal = createRegularUserPrincipal("Fry", "", listOf("USER"))
    val authentication = UsernamePasswordAuthenticationToken(principal, "", principal.authorities)
    return this.mutateWith(mockAuthentication(authentication))
}
