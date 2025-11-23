package io.orangebuffalo.simpleaccounting.infra.graphql

import graphql.schema.idl.RuntimeWiring
import graphql.validation.rules.OnValidationErrorStrategy
import graphql.validation.rules.ValidationRules
import graphql.validation.schemawiring.ValidationSchemaWiring
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for integrating graphql-java-extended-validation library.
 * This provides the validation instrumentation at runtime.
 */
@Configuration
class GraphQlValidationConfig {
    
    @Bean
    fun validationRules(): ValidationRules {
        return ValidationRules.newValidationRules()
            .onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL)
            .build()
    }
    
    @Bean
    fun validationSchemaWiring(): ValidationSchemaWiring {
        return ValidationSchemaWiring(validationRules())
    }
}
