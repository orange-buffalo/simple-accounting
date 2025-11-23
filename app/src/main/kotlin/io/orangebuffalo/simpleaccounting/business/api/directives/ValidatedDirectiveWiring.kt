package io.orangebuffalo.simpleaccounting.business.api.directives

import com.expediagroup.graphql.generator.directives.KotlinFieldDirectiveEnvironment
import com.expediagroup.graphql.generator.directives.KotlinSchemaDirectiveWiring
import graphql.schema.GraphQLFieldDefinition
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters

private val log = KotlinLogging.logger { }

/**
 * Registry to map field names to their containing classes for validation purposes.
 * This is populated during schema generation.
 */
object ValidationFunctionRegistry {
    private val registry = mutableMapOf<String, KClass<*>>()
    
    fun register(fieldName: String, kClass: KClass<*>) {
        registry[fieldName] = kClass
    }
    
    fun getKClass(fieldName: String): KClass<*>? = registry[fieldName]
}

/**
 * Directive wiring for the @validated directive.
 * This validates all arguments that have @notBlank or @maxLength annotations on the Kotlin function parameters.
 */
class ValidatedDirectiveWiring : KotlinSchemaDirectiveWiring {
    override fun onField(environment: KotlinFieldDirectiveEnvironment): GraphQLFieldDefinition {
        val directive = environment.directive
        check(directive.name == VALIDATED_DIRECTIVE_NAME) {
            "ValidatedDirectiveWiring can only be applied to the $VALIDATED_DIRECTIVE_NAME directive"
        }
        
        val originalDataFetcher = environment.getDataFetcher()
        val fieldDefinition = environment.element
        
        // Access Kotlin function using reflection
        // Try to get the class from registry, or look in all known mutation/query classes
        val fieldName = fieldDefinition.name
        val kClass = ValidationFunctionRegistry.getKClass(fieldName) 
            ?: findKClassForField(fieldName)
        
        val kFunction = kClass?.memberFunctions?.find { it.name == fieldName }
        
        if (kFunction == null) {
            log.warn { "Could not find Kotlin function for field $fieldName, validation will be skipped" }
        }
        
        environment.setDataFetcher { env ->
            log.trace { "Validating arguments for field ${fieldDefinition.name}" }
            
            // Collect all validation errors for this field
            val validationErrors = mutableListOf<FieldValidationError>()
            
            // Check each argument against Kotlin annotations
            kFunction?.valueParameters?.forEach { param ->
                val argName = param.name ?: return@forEach
                val argValue = env.getArgument<String?>(argName)
                
                // Check @NotBlank annotation
                val notBlankAnnotation = param.findAnnotation<NotBlank>()
                if (notBlankAnnotation != null && argValue.isNullOrBlank()) {
                    validationErrors.add(
                        FieldValidationError(
                            field = argName,
                            error = "MustNotBeBlank",
                            message = "must not be blank"
                        )
                    )
                }
                
                // Check @MaxLength annotation
                val maxLengthAnnotation = param.findAnnotation<MaxLength>()
                if (maxLengthAnnotation != null && argValue != null && argValue.length > maxLengthAnnotation.value) {
                    validationErrors.add(
                        FieldValidationError(
                            field = argName,
                            error = "SizeConstraintViolated",
                            message = "size must be between 0 and ${maxLengthAnnotation.value}",
                            params = mapOf("min" to "0", "max" to maxLengthAnnotation.value.toString())
                        )
                    )
                }
            }
            
            if (validationErrors.isNotEmpty()) {
                throw FieldValidationException(
                    message = "Validation failed",
                    validationErrors = validationErrors
                )
            }
            
            originalDataFetcher.get(env)
        }
        
        return environment.element
    }
    
    private fun findKClassForField(fieldName: String): KClass<*>? {
        // Scan all known mutation and query classes
        // This is a fallback when the registry doesn't have the entry
        val knownClasses = listOf(
            io.orangebuffalo.simpleaccounting.business.api.ChangePasswordMutation::class,
            io.orangebuffalo.simpleaccounting.business.api.RefreshAccessTokenMutation::class,
            io.orangebuffalo.simpleaccounting.business.api.UserProfileQuery::class,
        )
        
        return knownClasses.firstOrNull { kClass ->
            kClass.memberFunctions.any { it.name == fieldName }
        }
    }
}
