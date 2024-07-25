package io.orangebuffalo.simpleaccounting.business.security.remeberme

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface RefreshTokensRepository : AbstractEntityRepository<RefreshToken> {
    fun findByToken(token: String): RefreshToken?
}
