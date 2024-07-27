package io.orangebuffalo.simpleaccounting.infra.oauth2.impl

import io.orangebuffalo.simpleaccounting.infra.oauth2.PersistentOAuth2AuthorizedClientRepositoryExt
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PersistentOAuth2AuthorizedClientRepositoryExtImpl(
    private val dslContext: DSLContext
) : PersistentOAuth2AuthorizedClientRepositoryExt {

    private val client = Tables.PERSISTENT_OAUTH2_AUTHORIZED_CLIENT

    override fun deleteByClientRegistrationIdAndUserName(clientRegistrationId: String, userName: String) {
        dslContext.deleteFrom(client)
            .where(
                client.userName.eq(userName),
                client.clientRegistrationId.eq(clientRegistrationId)
            )
            .execute()
    }
}
