package io.orangebuffalo.simpleaccounting.services.integration.oauth2

import io.orangebuffalo.simpleaccounting.services.integration.oauth2.impl.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface PersistentOAuth2AuthorizedClientRepository
    : AbstractEntityRepository<PersistentOAuth2AuthorizedClient>, PersistentOAuth2AuthorizedClientRepositoryExt {

    fun findByClientRegistrationIdAndUserName(clientRegistrationId: String, userName: String):
            PersistentOAuth2AuthorizedClient?
}

interface PersistentOAuth2AuthorizedClientRepositoryExt {
    // todo #225: this query should be derived
    fun deleteByClientRegistrationIdAndUserName(clientRegistrationId: String, userName: String)
}
