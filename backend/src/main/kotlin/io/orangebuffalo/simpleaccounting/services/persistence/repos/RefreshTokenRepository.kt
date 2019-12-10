package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.RefreshToken

interface RefreshTokenRepository : AbstractEntityRepository<RefreshToken> {

    fun findByToken(token: String): RefreshToken?
}
