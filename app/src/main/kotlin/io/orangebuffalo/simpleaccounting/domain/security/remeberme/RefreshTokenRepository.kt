package io.orangebuffalo.simpleaccounting.domain.security.remeberme

import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface RefreshTokenRepository : AbstractEntityRepository<RefreshToken> {
    fun findByToken(token: String): RefreshToken?
}
