package io.orangebuffalo.simpleaccounting.business.oauthproviders

sealed class OAuthProviderCreationException(message: String) : RuntimeException(message) {

    /**
     * Exception thrown when a provider with the same name already exists.
     */
    class ProviderAlreadyExistsException(name: String) :
        OAuthProviderCreationException("OAuth provider with name '$name' already exists")
}

sealed class OAuthProviderUpdateException(message: String) : RuntimeException(message) {

    /**
     * Exception thrown when a provider with the same name already exists.
     */
    class ProviderAlreadyExistsException(name: String) :
        OAuthProviderUpdateException("OAuth provider with name '$name' already exists")

    /**
     * The attribute identifying the users at the provider defines the meaning of the already
     * linked external IDs, so it cannot be reinterpreted while the links exist.
     */
    class UserIdAttributeLockedException(linkedIdentitiesCount: Int) : OAuthProviderUpdateException(
        "User ID attribute cannot be changed while $linkedIdentitiesCount identities are linked to this provider"
    )
}
