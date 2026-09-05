package io.orangebuffalo.simpleaccounting.business.oauthproviders

/**
 * Failures of the OAuth2 based user authentication and identity linking flows.
 */
sealed class OAuthUserAuthenticationException(message: String) : RuntimeException(message) {

    /**
     * The user has no identity linked at the requested provider, so the login flow cannot be started.
     */
    class LoginNotAvailableException(userName: String, providerId: String) : OAuthUserAuthenticationException(
        "User '$userName' has no identity linked at provider $providerId"
    )

    /**
     * The user already has an identity linked at the requested provider.
     */
    class ProviderAlreadyLinkedException(providerId: String) : OAuthUserAuthenticationException(
        "An identity at provider $providerId is already linked to this user"
    )

    /**
     * The authorization server callback cannot be matched to a pending authorization request,
     * or is presented by a browser that has not started the flow.
     */
    class UnknownAuthorizationRequestException : OAuthUserAuthenticationException(
        "Authorization request is not known or has expired"
    )

    /**
     * The authorization server did not grant the access, or the token exchange failed.
     */
    class AuthorizationFailedException(reason: String) : OAuthUserAuthenticationException(
        "Authorization has not been granted: $reason"
    )

    /**
     * The provider reported an identity that is not the one linked to the user being logged in.
     */
    class IdentityMismatchException : OAuthUserAuthenticationException(
        "The authenticated identity does not match the identity linked to this user"
    )

    /**
     * The provider reported an identity that is already linked to another user.
     */
    class IdentityAlreadyInUseException : OAuthUserAuthenticationException(
        "The authenticated identity is already linked to another user"
    )
}
