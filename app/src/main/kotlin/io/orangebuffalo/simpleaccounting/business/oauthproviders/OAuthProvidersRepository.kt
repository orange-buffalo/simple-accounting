package io.orangebuffalo.simpleaccounting.business.oauthproviders

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface OAuthProvidersRepository : AbstractEntityRepository<OAuthProvider> {
    /**
     * Names that only differ in case are too easy to confuse for the users choosing
     * a provider on the login page, hence they are treated as duplicates.
     */
    fun findByNameIgnoreCase(name: String): OAuthProvider?
}
