package io.orangebuffalo.simpleaccounting.business.api.directives

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions

/**
 * Finds the Kotlin function for a given field name by scanning known mutation and query classes.
 */
internal fun findKFunctionForField(fieldName: String): KFunction<*>? {
    val kClass = findKClassForField(fieldName)
    return kClass?.memberFunctions?.find { it.name == fieldName }
}

/**
 * Finds the Kotlin class that contains a field with the given name.
 */
private fun findKClassForField(fieldName: String): KClass<*>? {
    // Scan all known mutation and query classes
    val knownClasses = listOf(
        io.orangebuffalo.simpleaccounting.business.api.ChangePasswordMutation::class,
        io.orangebuffalo.simpleaccounting.business.api.RefreshAccessTokenMutation::class,
        io.orangebuffalo.simpleaccounting.business.api.UserProfileQuery::class,
    )
    
    return knownClasses.firstOrNull { kClass ->
        kClass.memberFunctions.any { it.name == fieldName }
    }
}
