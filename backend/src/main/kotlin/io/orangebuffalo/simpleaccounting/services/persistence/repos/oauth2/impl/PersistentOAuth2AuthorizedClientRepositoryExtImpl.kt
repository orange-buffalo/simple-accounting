package io.orangebuffalo.simpleaccounting.services.persistence.repos.oauth2.impl

import io.orangebuffalo.simpleaccounting.services.persistence.entities.oauth2.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.services.persistence.fetchOneOrNull
import io.orangebuffalo.simpleaccounting.services.persistence.model.Tables
import io.orangebuffalo.simpleaccounting.services.persistence.repos.oauth2.PersistentOAuth2AuthorizedClientRepositoryExt
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

    override fun findByClientRegistrationIdAndUserName(
        clientRegistrationId: String,
        userName: String
    ): PersistentOAuth2AuthorizedClient? = dslContext
        .select()
        .from(client)
        .where(
            client.userName.eq(userName),
            client.clientRegistrationId.eq(clientRegistrationId)
        )
        .fetchOneOrNull()
}
