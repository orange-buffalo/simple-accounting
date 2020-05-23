package io.orangebuffalo.simpleaccounting.services.persistence.repos.oauth2

import io.orangebuffalo.simpleaccounting.services.persistence.entities.oauth2.PersistentOAuth2AuthorizationRequest
import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository

interface Oauth2AuthorizationRequestRepository
    : AbstractEntityRepository<PersistentOAuth2AuthorizationRequest>, Oauth2AuthorizationRequestRepositoryExt

interface Oauth2AuthorizationRequestRepositoryExt {
    fun findByState(state: String): PersistentOAuth2AuthorizationRequest?
}
