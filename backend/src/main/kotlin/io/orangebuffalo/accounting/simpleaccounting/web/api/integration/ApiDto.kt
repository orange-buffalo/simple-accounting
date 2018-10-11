package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ApiDto(val dtoClass: KClass<*>)