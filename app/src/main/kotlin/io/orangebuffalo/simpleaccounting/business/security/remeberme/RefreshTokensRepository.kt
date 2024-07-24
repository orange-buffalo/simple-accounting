package io.orangebuffalo.simpleaccounting.business.security.remeberme

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface RefreshTokensRepository : AbstractEntityRepository<RefreshToken> {
    fun findByToken(token: String): RefreshToken?
}
