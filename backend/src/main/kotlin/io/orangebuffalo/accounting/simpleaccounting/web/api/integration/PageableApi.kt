package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PageableApi(val descriptorClass: KClass<out PageableApiDescriptor<*>>)

interface PageableApiDescriptor<in E> {
    fun mapEntityToDto(entity: E) : Any
}