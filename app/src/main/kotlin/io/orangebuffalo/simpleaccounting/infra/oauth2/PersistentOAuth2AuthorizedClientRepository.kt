package io.orangebuffalo.simpleaccounting.infra.oauth2

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository
import io.orangebuffalo.simpleaccounting.infra.oauth2.impl.PersistentOAuth2AuthorizedClient

interface PersistentOAuth2AuthorizedClientRepository
    : AbstractEntityRepository<PersistentOAuth2AuthorizedClient>, PersistentOAuth2AuthorizedClientRepositoryExt {

    fun findByClientRegistrationIdAndUserName(clientRegistrationId: String, userName: String):
            PersistentOAuth2AuthorizedClient?
}

interface PersistentOAuth2AuthorizedClientRepositoryExt {
    // deriving delete queries is not yet supported: https://jira.spring.io/browse/DATAJDBC-551
    fun deleteByClientRegistrationIdAndUserName(clientRegistrationId: String, userName: String)
}
