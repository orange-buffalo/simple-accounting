package io.orangebuffalo.simpleaccounting.services.persistence.repos.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.PlatformUserRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PlatformUserRepositoryExtImpl(
    private val dslContext: DSLContext
) : PlatformUserRepositoryExt {

    private val user = Tables.PLATFORM_USER

    override fun findByUserName(userName: String): PlatformUser? = dslContext
        .select()
        .from(user)
        .where(user.userName.eq(userName))
        .fetchOneOrNull()
}
