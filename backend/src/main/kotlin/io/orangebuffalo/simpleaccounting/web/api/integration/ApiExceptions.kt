package io.orangebuffalo.simpleaccounting.web.api.integration

class ApiValidationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class EntityNotFoundException(message: String) : RuntimeException(message)
