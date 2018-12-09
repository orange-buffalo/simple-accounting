package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import org.springframework.beans.factory.BeanFactory
import org.springframework.stereotype.Component
import java.lang.reflect.AnnotatedElement

@Component
class PageableApiDescriptorResolver(
    private val beanFactory: BeanFactory
) {

    fun resolveDescriptor(annotatedElement: AnnotatedElement): PageableApiDescriptor<*, *> {
        val pageableApi = annotatedElement.getAnnotation(PageableApi::class.java)
            ?: throw IllegalArgumentException("Missing @PageableApi at $annotatedElement")

        return beanFactory.getBean(pageableApi.descriptorClass.java)
    }
}