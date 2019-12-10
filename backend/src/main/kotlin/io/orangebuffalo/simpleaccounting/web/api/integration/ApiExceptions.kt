package io.orangebuffalo.simpleaccounting.web.api.integration

class ApiValidationException(message: String) : RuntimeException(message)

class EntityNotFoundException(message: String) : RuntimeException(message)
