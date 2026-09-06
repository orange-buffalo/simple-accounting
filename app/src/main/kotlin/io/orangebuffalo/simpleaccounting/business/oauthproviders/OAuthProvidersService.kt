package io.orangebuffalo.simpleaccounting.business.oauthproviders

import io.orangebuffalo.simpleaccounting.business.common.exceptions.EntityNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

/**
 * Manages the OAuth2 providers registered by the admins.
 */
@Service
class OAuthProvidersService(
    private val providersRepository: OAuthProvidersRepository,
    private val identitiesRepository: UserOAuthIdentitiesRepository,
) {

    fun getProviderById(providerId: String): OAuthProvider = providersRepository.findByIdOrNull(providerId)
        ?: throw EntityNotFoundException("OAuth provider $providerId is not found")

    fun getProviders(): List<OAuthProvider> = providersRepository.findAll().toList()

    /**
     * Registers a new provider.
     * @throws OAuthProviderCreationException.ProviderAlreadyExistsException in case a provider with the same name exists
     */
    fun createProvider(provider: OAuthProvider): OAuthProvider {
        if (providersRepository.findByNameIgnoreCase(provider.name) != null) {
            throw OAuthProviderCreationException.ProviderAlreadyExistsException(provider.name)
        }
        return saveWithUniqueName(provider) {
            OAuthProviderCreationException.ProviderAlreadyExistsException(provider.name)
        }
    }

    /**
     * Validates and saves updated provider data.
     * @throws OAuthProviderUpdateException.ProviderAlreadyExistsException in case another provider with the same name exists
     * @throws OAuthProviderUpdateException.UserIdAttributeLockedException in case identities are already linked
     */
    fun updateProvider(updatedProviderData: OAuthProvider): OAuthProvider {
        val providerId = checkNotNull(updatedProviderData.id) {
            "Provider id must be provided for update"
        }
        val existingProvider = providersRepository.findByNameIgnoreCase(updatedProviderData.name)
        if (existingProvider != null && existingProvider.id != providerId) {
            throw OAuthProviderUpdateException.ProviderAlreadyExistsException(updatedProviderData.name)
        }

        val currentProvider = getProviderById(providerId)
        if (currentProvider.userIdAttribute != updatedProviderData.userIdAttribute) {
            val linkedIdentities = identitiesRepository.findByProviderId(providerId)
            if (linkedIdentities.isNotEmpty()) {
                throw OAuthProviderUpdateException.UserIdAttributeLockedException(linkedIdentities.size)
            }
        }

        return saveWithUniqueName(updatedProviderData) {
            OAuthProviderUpdateException.ProviderAlreadyExistsException(updatedProviderData.name)
        }
    }

    /**
     * The name check above is not atomic. The unique constraint is the actual arbiter between
     * concurrent registrations of the very same name; its failure is reported as the same
     * business error instead of leaking a persistence failure.
     */
    private fun saveWithUniqueName(provider: OAuthProvider, exceptionProvider: () -> RuntimeException): OAuthProvider =
        try {
            providersRepository.save(provider)
        } catch (e: DbActionExecutionException) {
            if (e.cause is DuplicateKeyException) throw exceptionProvider() else throw e
        } catch (_: DuplicateKeyException) {
            throw exceptionProvider()
        }
}
