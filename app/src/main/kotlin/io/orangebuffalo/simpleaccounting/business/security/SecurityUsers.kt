package io.orangebuffalo.simpleaccounting.business.security

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails

/**
 * Application security principal, describing currently logged in user.
 */
interface SecurityPrincipal {

    /**
     * Returns current user username for regular user. For transient users, returns token they have logged in with.
     * @see isTransient
     */
    val userName: String

    /**
     * If transient, no [PlatformUser]
     * exists for current principal. This means current user is logged in via a shared
     * workspace token and [userName] should return the token user used
     * to login with.
     * Otherwise a user with [userName] should be registered in the system.
     */
    val isTransient: Boolean

    /**
     * Returns current user roles.
     */
    val roles: Collection<String>
}

fun createRegularUserPrincipal(userName: String, password: String, roles: Collection<String>): SpringSecurityPrincipal =
    SecurityPrincipalImpl(
        userName = userName,
        password = password,
        authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") },
        isTransient = false
    )

fun createTransientUserPrincipal(sharedWorkspaceToken: String, password: String? = null): SpringSecurityPrincipal =
    SecurityPrincipalImpl(
        userName = sharedWorkspaceToken,
        password = password ?: sharedWorkspaceToken,
        authorities = listOf(SimpleGrantedAuthority("ROLE_${SaUserRoles.USER}")),
        isTransient = true
    )

/**
 * A bridge from [SecurityPrincipal] to Spring Security API.
 */
interface SpringSecurityPrincipal : SecurityPrincipal, UserDetails

fun PlatformUser.toSecurityPrincipal(): SpringSecurityPrincipal = createRegularUserPrincipal(
    userName = this.userName,
    password = this.passwordHash,
    roles = listOf(if (isAdmin) SaUserRoles.ADMIN else SaUserRoles.USER)
)

private class SecurityPrincipalImpl(
    override val userName: String,
    password: String?,
    authorities: Collection<GrantedAuthority>?,
    override val isTransient: Boolean

) : User(userName, password ?: "", authorities ?: emptyList()), SpringSecurityPrincipal {

    override val roles: Collection<String> = authorities
        ?.asSequence()
        ?.filter { it.authority?.startsWith("ROLE_") == true }
        ?.map { it.authority?.removePrefix("ROLE_") ?: "" }
        ?.toList()
        ?: emptyList()
}

class InsufficientUserType : RuntimeException()

object SaUserRoles {
    const val ADMIN = "ADMIN"
    const val USER = "USER"
}
