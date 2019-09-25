package io.orangebuffalo.accounting.simpleaccounting

import io.orangebuffalo.accounting.simpleaccounting.services.security.createRegularUserPrincipal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContext
import org.springframework.security.test.context.support.WithSecurityContextFactory

@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = SaMockSecurityContextFactory::class)
annotation class WithSaMockUser(
    val userName: String = "",
    val roles: Array<String> = []
)

@Retention(AnnotationRetention.RUNTIME)
@WithSaMockUser(roles = ["USER"], userName = "Fry")
annotation class WithMockFryUser

@Retention(AnnotationRetention.RUNTIME)
@WithSaMockUser(roles = ["USER"], userName = "Zoidberg")
annotation class WithMockZoidbergUser

@Retention(AnnotationRetention.RUNTIME)
@WithSaMockUser(roles = ["USER"], userName = "Farnsworth")
annotation class WithMockFarnsworthUser

@Retention(AnnotationRetention.RUNTIME)
@WithSaMockUser(roles = ["USER"], userName = "Roberto")
annotation class WithMockRobertoUser

@Retention(AnnotationRetention.RUNTIME)
@WithSaMockUser(roles = ["USER"], userName = "MafiaBot")
annotation class WithMockMafiaBotUser

class SaMockSecurityContextFactory : WithSecurityContextFactory<WithSaMockUser> {
    override fun createSecurityContext(withSaMockUser: WithSaMockUser): SecurityContext {
        val context = SecurityContextHolder.createEmptyContext()
        val principal = createRegularUserPrincipal(withSaMockUser.userName, "", withSaMockUser.roles.asList())
        context.authentication = UsernamePasswordAuthenticationToken(principal, "", principal.authorities)
        return context
    }
}