package io.orangebuffalo.simpleaccounting.web.api.integration.errorhandling

class ApiValidationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
