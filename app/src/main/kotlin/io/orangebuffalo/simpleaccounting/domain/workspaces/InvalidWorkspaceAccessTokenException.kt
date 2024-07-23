package io.orangebuffalo.simpleaccounting.domain.workspaces

class InvalidWorkspaceAccessTokenException(token: String) : RuntimeException("Token $token is not valid")
