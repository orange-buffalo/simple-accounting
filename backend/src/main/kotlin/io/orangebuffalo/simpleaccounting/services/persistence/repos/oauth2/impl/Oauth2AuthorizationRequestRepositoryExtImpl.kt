package io.orangebuffalo.simpleaccounting.services.persistence.repos.oauth2.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.oauth2.PersistentOAuth2AuthorizationRequest
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.oauth2.Oauth2AuthorizationRequestRepositoryExt
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class Oauth2AuthorizationRequestRepositoryExtImpl(
   private val dslContext: DSLContext
) : Oauth2AuthorizationRequestRepositoryExt {

    private val request = Tables.PERSISTENT_OAUTH2_AUTHORIZATION_REQUEST

    override fun findByState(state: String): PersistentOAuth2AuthorizationRequest? =   dslContext
        .select()
        .from(request)
        .where(request.state.eq(state))
        .fetchOneOrNull()
}
