package io.orangebuffalo.accounting.simpleaccounting.services.security

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails

fun PlatformUser.toUserDetails(): UserDetails = User.builder()
    .username(userName)
    .password(passwordHash)
    .accountExpired(false)
    .accountLocked(false)
    .credentialsExpired(false)
    .roles(if (isAdmin) "ADMIN" else "USER")
    .build()