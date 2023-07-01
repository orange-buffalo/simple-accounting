package io.orangebuffalo.simpleaccounting.services.business

class InvalidWorkspaceAccessTokenException(token: String) : RuntimeException("Token $token is not valid")
