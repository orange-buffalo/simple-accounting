package io.orangebuffalo.simpleaccounting.services.persistence.repos.oauth2

import io.orangebuffalo.simpleaccounting.services.persistence.entities.oauth2.PersistentOAuth2AuthorizationRequest
import io.orangebuffalo.simpleaccounting.services.persistence.repos.LegacyAbstractEntityRepository

interface Oauth2AuthorizationRequestRepository : LegacyAbstractEntityRepository<PersistentOAuth2AuthorizationRequest> {

    fun findByState(state: String): PersistentOAuth2AuthorizationRequest?

}
