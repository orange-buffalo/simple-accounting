package io.orangebuffalo.accounting.simpleaccounting.services.persistence

import com.querydsl.core.types.*
import org.springframework.data.domain.Sort

fun <T : Comparable<*>> OrderSpecifier<T>.toSort(): Sort {
    val propertyName = this.target.accept(object : Visitor<String?, Void?> {
        override fun visit(expr: Path<*>?, context: Void?): String? {
            return expr?.metadata?.name
        }

        override fun visit(expr: FactoryExpression<*>?, context: Void?): String? = null
        override fun visit(expr: Operation<*>?, context: Void?): String? = null
        override fun visit(expr: ParamExpression<*>?, context: Void?): String? = null
        override fun visit(expr: SubQueryExpression<*>?, context: Void?): String? = null
        override fun visit(expr: TemplateExpression<*>?, context: Void?): String? = null
        override fun visit(expr: Constant<*>?, context: Void?): String? = null
    }, null)!!

    return Sort.by(Sort.Order.by(propertyName).with(Sort.Direction.fromString(this.order.name)))
}