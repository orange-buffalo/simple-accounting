package io.orangebuffalo.simpleaccounting.business.security.remeberme

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface RefreshTokenRepository : AbstractEntityRepository<RefreshToken> {
    fun findByToken(token: String): RefreshToken?
}
