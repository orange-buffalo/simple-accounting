package io.orangebuffalo.simpleaccounting.business.workspaces

class InvalidWorkspaceAccessTokenException(token: String) : RuntimeException("Token $token is not valid")
