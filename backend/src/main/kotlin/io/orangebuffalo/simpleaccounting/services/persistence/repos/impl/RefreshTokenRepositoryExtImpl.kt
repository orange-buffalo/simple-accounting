package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.RefreshToken
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.RefreshTokenRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class RefreshTokenRepositoryExtImpl(
    private val dslContext: DSLContext
) : RefreshTokenRepositoryExt {

    private val refreshToken = Tables.REFRESH_TOKEN

    override fun findByToken(token: String): RefreshToken? = dslContext
        .select()
        .from(refreshToken)
        .where(refreshToken.token.eq(token))
        .fetchOneOrNull()
}
