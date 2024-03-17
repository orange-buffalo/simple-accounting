package io.orangebuffalo.simpleaccounting.domain.users

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface UserActivationTokenRepository : AbstractEntityRepository<UserActivationToken> {

    fun findByToken(token: String): UserActivationToken?

    fun findByUserId(userId: Long): UserActivationToken?
}
