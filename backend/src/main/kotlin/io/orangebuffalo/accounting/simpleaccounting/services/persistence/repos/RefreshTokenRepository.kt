package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.RefreshToken

interface RefreshTokenRepository : AbstractEntityRepository<RefreshToken> {

    fun findByToken(token: String): RefreshToken?
}