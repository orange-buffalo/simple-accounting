package io.orangebuffalo.accounting.simpleaccounting.services.business

class InvalidWorkspaceAccessTokenException(token: String) : RuntimeException("Token $token is not valid")