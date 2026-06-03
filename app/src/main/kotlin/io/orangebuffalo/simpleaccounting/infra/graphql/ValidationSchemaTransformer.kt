package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import graphql.Scalars
import graphql.introspection.Introspection
import graphql.language.IntValue
import graphql.schema.*
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorCode
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorDetails
import io.orangebuffalo.simpleaccounting.business.api.errors.ValidationErrorParam
import jakarta.validation.ConstraintViolation
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberFunctions

/**
 * Registry entry that defines how a Jakarta validation annotation maps to a GraphQL directive
 * and how constraint violations are transformed into error responses.
 */
open class ValidationDirectiveMapping(
    val annotationClass: KClass<out Annotation>,
    val directiveName: String,
    val directiveDescription: String,
    val directiveArgumentsBuilder: (GraphQLDirective.Builder) -> Unit = {},
    val appliedDirectiveArgumentsBuilder: (GraphQLAppliedDirective.Builder, Annotation) -> Unit = { _, _ -> },
    val errorCode: ValidationErrorCode,
    val paramsExtractor: ((ConstraintViolation<*>) -> List<ValidationErrorParam>)? = null,
    val runtimeValidator: (path: String, value: Any?, directive: GraphQLAppliedDirective) -> ValidationErrorDetails? = { _, _, _ -> null },
) {
    fun buildAppliedDirective(annotation: Annotation): GraphQLAppliedDirective =
        GraphQLAppliedDirective.newDirective()
            .name(directiveName)
            .apply { appliedDirectiveArgumentsBuilder(this, annotation) }
            .build()
}

@Component
class NotBlankValidationDirective : ValidationDirectiveMapping(
        annotationClass = NotBlank::class,
        directiveName = "notBlank",
        directiveDescription = "Validates that the string is not null, empty, or blank",
        errorCode = ValidationErrorCode.MustNotBeBlank,
        runtimeValidator = { path, value, _ ->
            val stringValue = value as? String
            if (stringValue == null || stringValue.isBlank()) {
                ValidationErrorDetails(
                    path = path,
                    error = ValidationErrorCode.MustNotBeBlank,
                    message = "must not be blank",
                )
            } else {
                null
            }
        }
)

@Component
class SizeValidationDirective : ValidationDirectiveMapping(
        annotationClass = Size::class,
        directiveName = "size",
        directiveDescription = "Validates the size of a string",
        directiveArgumentsBuilder = { builder ->
            builder.argument(
                GraphQLArgument.newArgument()
                    .name("min")
                    .type(GraphQLNonNull(Scalars.GraphQLInt))
                    .description("Minimum size")
                    .build()
            ).argument(
                GraphQLArgument.newArgument()
                    .name("max")
                    .type(GraphQLNonNull(Scalars.GraphQLInt))
                    .description("Maximum size")
                    .build()
            )
        },
        appliedDirectiveArgumentsBuilder = { builder, annotation ->
            val size = annotation as Size
            builder.argument(
                GraphQLAppliedDirectiveArgument.newArgument()
                    .name("min")
                    .valueLiteral(IntValue.newIntValue(size.min.toBigInteger()).build())
                    .type(Scalars.GraphQLInt)
                    .build()
            ).argument(
                GraphQLAppliedDirectiveArgument.newArgument()
                    .name("max")
                    .valueLiteral(IntValue.newIntValue(size.max.toBigInteger()).build())
                    .type(Scalars.GraphQLInt)
                    .build()
            )
        },
        errorCode = ValidationErrorCode.SizeConstraintViolated,
        paramsExtractor = { violation ->
            val attributes = violation.constraintDescriptor.attributes
            listOf(
                ValidationErrorParam("min", attributes["min"]?.toString() ?: "0"),
                ValidationErrorParam("max", attributes["max"]?.toString() ?: "2147483647")
            )
        },
        runtimeValidator = { path, value, directive ->
            val stringValue = value as? String
            val min = directive.intArgument("min") ?: 0
            val max = directive.intArgument("max") ?: Int.MAX_VALUE
            if (stringValue != null && (stringValue.length < min || stringValue.length > max)) {
                ValidationErrorDetails(
                    path = path,
                    error = ValidationErrorCode.SizeConstraintViolated,
                    message = "size must be between $min and $max",
                    params = listOf(
                        ValidationErrorParam("min", min.toString()),
                        ValidationErrorParam("max", max.toString()),
                    )
                )
            } else {
                null
            }
        }
)

@Component
class MaxValidationDirective : ValidationDirectiveMapping(
        annotationClass = Max::class,
        directiveName = "max",
        directiveDescription = "Validates that the value is at most the specified maximum",
        directiveArgumentsBuilder = { builder ->
            builder.argument(
                GraphQLArgument.newArgument()
                    .name("value")
                    .type(GraphQLNonNull(Scalars.GraphQLInt))
                    .description("Maximum value")
                    .build()
            )
        },
        appliedDirectiveArgumentsBuilder = { builder, annotation ->
            val max = annotation as Max
            builder.argument(
                GraphQLAppliedDirectiveArgument.newArgument()
                    .name("value")
                    .valueLiteral(IntValue.newIntValue(max.value.toBigInteger()).build())
                    .type(Scalars.GraphQLInt)
                    .build()
            )
        },
        errorCode = ValidationErrorCode.MaxConstraintViolated,
        paramsExtractor = { violation ->
            val attributes = violation.constraintDescriptor.attributes
            listOf(
                ValidationErrorParam("value", attributes["value"]?.toString() ?: "")
            )
        },
        runtimeValidator = { path, value, directive ->
            val numberValue = (value as? Number)?.toLong()
            val max = directive.intArgument("value")
            if (numberValue != null && max != null && numberValue > max) {
                ValidationErrorDetails(
                    path = path,
                    error = ValidationErrorCode.MaxConstraintViolated,
                    message = "must be less than or equal to $max",
                    params = listOf(ValidationErrorParam("value", max.toString())),
                )
            } else {
                null
            }
        }
)

@Component
class MinValidationDirective : ValidationDirectiveMapping(
        annotationClass = Min::class,
        directiveName = "min",
        directiveDescription = "Validates that the value is at least the specified minimum",
        directiveArgumentsBuilder = { builder ->
            builder.argument(
                GraphQLArgument.newArgument()
                    .name("value")
                    .type(GraphQLNonNull(Scalars.GraphQLInt))
                    .description("Minimum value")
                    .build()
            )
        },
        appliedDirectiveArgumentsBuilder = { builder, annotation ->
            val min = annotation as Min
            builder.argument(
                GraphQLAppliedDirectiveArgument.newArgument()
                    .name("value")
                    .valueLiteral(IntValue.newIntValue(min.value.toBigInteger()).build())
                    .type(Scalars.GraphQLInt)
                    .build()
            )
        },
        errorCode = ValidationErrorCode.MinConstraintViolated,
        paramsExtractor = { violation ->
            val attributes = violation.constraintDescriptor.attributes
            listOf(
                ValidationErrorParam("value", attributes["value"]?.toString() ?: "")
            )
        },
        runtimeValidator = { path, value, directive ->
            val numberValue = (value as? Number)?.toLong()
            val min = directive.intArgument("value")
            if (numberValue != null && min != null && numberValue < min) {
                ValidationErrorDetails(
                    path = path,
                    error = ValidationErrorCode.MinConstraintViolated,
                    message = "must be greater than or equal to $min",
                    params = listOf(ValidationErrorParam("value", min.toString())),
                )
            } else {
                null
            }
        }
)

internal fun GraphQLAppliedDirective.intArgument(name: String): Int? = getArgument(name)?.getValue<Int>()

/**
 * Transforms the GraphQL schema by adding validation directives based on Jakarta validation annotations.
 * This makes validation constraints visible in the schema for API documentation purposes.
 */
@Component
class ValidationSchemaTransformer(
    validationDirectiveMappings: List<ValidationDirectiveMapping>,
) {

    private val validationDirectiveMappings = validationDirectiveMappings.sortedBy { it.directiveName }
    private val mappingsByAnnotationClass: Map<KClass<out Annotation>, ValidationDirectiveMapping> =
        validationDirectiveMappings.associateBy { it.annotationClass }

    fun transform(schema: GraphQLSchema, mutations: List<Mutation>, queries: List<Query>): GraphQLSchema {
        val appliedDirectivesMap = buildAppliedDirectivesMap(mutations, queries)

        if (appliedDirectivesMap.isEmpty()) {
            return schema
        }

        val usedDirectiveNames = appliedDirectivesMap.values
            .flatMap { it.values }
            .flatten()
            .map { it.name }
            .toSet()

        return schema.transform { schemaBuilder ->
            // Add only the directive definitions that are actually used
            validationDirectiveMappings
                .filter { it.directiveName in usedDirectiveNames }
                .forEach { mapping ->
                    if (schema.getDirective(mapping.directiveName) == null) {
                        schemaBuilder.additionalDirective(createDirective(mapping))
                    }
                }

            schema.mutationType?.let { mutationType ->
                schemaBuilder.mutation(transformObjectType(mutationType, appliedDirectivesMap))
            }

            schema.queryType?.let { queryType ->
                schemaBuilder.query(transformObjectType(queryType, appliedDirectivesMap))
            }
        }
    }

    private fun buildAppliedDirectivesMap(
        mutations: List<Mutation>,
        queries: List<Query>
    ): Map<String, Map<String, List<GraphQLAppliedDirective>>> {
        val result = mutableMapOf<String, Map<String, List<GraphQLAppliedDirective>>>()

        for (kClass in (mutations + queries).map { it::class }) {
            for (function in kClass.memberFunctions) {
                val argumentDirectives = mutableMapOf<String, List<GraphQLAppliedDirective>>()

                for (param in function.parameters) {
                    if (param.kind == KParameter.Kind.INSTANCE) continue

                    val directives = param.annotations.mapNotNull { annotation ->
                        mappingsByAnnotationClass[annotation.annotationClass]
                            ?.buildAppliedDirective(annotation)
                    }

                    if (directives.isNotEmpty()) {
                        argumentDirectives[param.name ?: continue] = directives
                    }
                }

                if (argumentDirectives.isNotEmpty()) {
                    result[function.name] = argumentDirectives
                }
            }
        }

        return result
    }

    private fun transformObjectType(
        objectType: GraphQLObjectType,
        appliedDirectivesMap: Map<String, Map<String, List<GraphQLAppliedDirective>>>
    ): GraphQLObjectType {
        return objectType.transform { typeBuilder ->
            val transformedFields = objectType.fieldDefinitions.map { field ->
                val argumentDirectives = appliedDirectivesMap[field.name]
                if (argumentDirectives != null) {
                    transformField(field, argumentDirectives)
                } else {
                    field
                }
            }
            typeBuilder.clearFields().fields(transformedFields)
        }
    }

    private fun transformField(
        field: GraphQLFieldDefinition,
        argumentDirectives: Map<String, List<GraphQLAppliedDirective>>
    ): GraphQLFieldDefinition {
        return field.transform { fieldBuilder ->
            val transformedArguments = field.arguments.map { argument ->
                val directives = argumentDirectives[argument.name]
                if (directives != null) {
                    argument.transform { argBuilder ->
                        directives.forEach { argBuilder.withAppliedDirective(it) }
                    }
                } else {
                    argument
                }
            }
            fieldBuilder.clearArguments().arguments(transformedArguments)
        }
    }

    private fun createDirective(mapping: ValidationDirectiveMapping): GraphQLDirective {
        return GraphQLDirective.newDirective()
            .name(mapping.directiveName)
            .description(mapping.directiveDescription)
            .validLocation(Introspection.DirectiveLocation.ARGUMENT_DEFINITION)
            .validLocation(Introspection.DirectiveLocation.INPUT_FIELD_DEFINITION)
            .apply { mapping.directiveArgumentsBuilder(this) }
            .build()
    }
}
