package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface UserActivationTokensRepository : AbstractEntityRepository<UserActivationToken> {

    fun findByToken(token: String): UserActivationToken?

    fun findByUserId(userId: Long): UserActivationToken?
}
