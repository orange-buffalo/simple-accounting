package io.orangebuffalo.simpleaccounting.business.oauthproviders

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntityRepository

interface UserOAuthIdentitiesRepository : AbstractEntityRepository<UserOAuthIdentity> {
    fun findByUserId(userId: String): List<UserOAuthIdentity>

    fun findByUserIdAndProviderId(userId: String, providerId: String): UserOAuthIdentity?

    fun findByProviderIdAndExternalId(providerId: String, externalId: String): UserOAuthIdentity?

    fun findByProviderId(providerId: String): List<UserOAuthIdentity>
}
