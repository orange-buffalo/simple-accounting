package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import com.expediagroup.graphql.generator.annotations.GraphQLName
import graphql.Scalars
import graphql.schema.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

/**
 * Supports schema generation for generic [ConnectionGqlDto] and [EdgeGqlDto] types.
 *
 * Since graphql-kotlin does not support generic type parameters, this class intercepts
 * the type generation process and creates concrete GraphQL types based on the generic
 * parameter (e.g., `ConnectionGqlDto<WorkspaceGqlDto>` → `WorkspacesConnection` + `WorkspaceEdge`).
 *
 * Used by [io.orangebuffalo.simpleaccounting.infra.graphql.SaSchemaGeneratorHooks].
 */
class ConnectionSchemaGenerationSupport {

    private val pendingEdgeTypes = mutableListOf<GraphQLObjectType>()

    /**
     * Pre-scans query and mutation classes to find all node types used in [ConnectionGqlDto]
     * return types (including nested types). These node types must be added to the schema's
     * `additionalTypes` to ensure they are generated, since the hook-generated connection types
     * reference them via [GraphQLTypeReference] and graphql-kotlin would not encounter them otherwise.
     *
     * Also includes [PageInfoGqlDto] which is referenced by all generated connection types.
     */
    fun collectAdditionalTypes(operations: List<Any>): Set<KType> {
        val nodeTypes = mutableSetOf<KType>()
        val processed = mutableSetOf<KClass<*>>()
        for (operation in operations) {
            collectFromClass(operation::class, nodeTypes, processed)
        }
        if (nodeTypes.isNotEmpty()) {
            nodeTypes.add(PageInfoGqlDto::class.createType())
        }
        return nodeTypes
    }

    private fun collectFromClass(klass: KClass<*>, nodeTypes: MutableSet<KType>, processed: MutableSet<KClass<*>>) {
        if (!processed.add(klass)) return
        for (func in klass.memberFunctions) {
            collectFromType(func.returnType, nodeTypes, processed)
        }
        for (prop in klass.memberProperties) {
            collectFromType(prop.returnType, nodeTypes, processed)
        }
    }

    private fun collectFromType(type: KType, nodeTypes: MutableSet<KType>, processed: MutableSet<KClass<*>>) {
        val classifier = type.classifier as? KClass<*> ?: return
        if (classifier == ConnectionGqlDto::class) {
            type.arguments.firstOrNull()?.type?.let { nodeTypes.add(it) }
            return
        }
        if (classifier.qualifiedName?.startsWith("io.orangebuffalo.simpleaccounting") == true) {
            collectFromClass(classifier, nodeTypes, processed)
        }
        type.arguments.forEach { arg ->
            arg.type?.let { collectFromType(it, nodeTypes, processed) }
        }
    }

    /**
     * Called from [io.orangebuffalo.simpleaccounting.infra.graphql.SaSchemaGeneratorHooks.willGenerateGraphQLType].
     * Returns a concrete GraphQL type if the given [type] is a [ConnectionGqlDto],
     * or `null` to let normal generation proceed.
     */
    fun willGenerateGraphQLType(type: KType): GraphQLType? {
        val classifier = type.classifier as? KClass<*> ?: return null
        if (classifier != ConnectionGqlDto::class) return null

        val nodeClass = type.arguments.firstOrNull()?.type?.classifier as? KClass<*> ?: return null
        val nodeName = getGraphQLName(nodeClass)
        val nodePluralName = pluralize(nodeName)
        val connectionName = "${nodePluralName}Connection"
        val edgeName = "${nodeName}Edge"
        val nodeNameHuman = toHumanReadable(nodeName)
        val nodePluralNameHuman = toHumanReadable(nodePluralName)

        val edgeType = GraphQLObjectType.newObject()
            .name(edgeName)
            .description("An edge in a $nodePluralNameHuman connection.")
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("cursor")
                    .description("The cursor of this edge, which can be used for pagination.")
                    .type(GraphQLNonNull(Scalars.GraphQLString))
                    .build()
            )
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("node")
                    .description("The $nodeNameHuman at the end of this edge.")
                    .type(GraphQLNonNull(GraphQLTypeReference(nodeName)))
                    .build()
            )
            .build()

        pendingEdgeTypes.add(edgeType)

        return GraphQLObjectType.newObject()
            .name(connectionName)
            .description("A paginated connection of $nodePluralNameHuman following the GraphQL Cursor Connections Specification.")
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("edges")
                    .description("The list of edges in the current page.")
                    .type(GraphQLNonNull(GraphQLList(GraphQLNonNull(GraphQLTypeReference(edgeName)))))
                    .build()
            )
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("pageInfo")
                    .description("Pagination information about the current page.")
                    .type(GraphQLNonNull(GraphQLTypeReference("PageInfo")))
                    .build()
            )
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("totalCount")
                    .description("The total number of items in the connection across all pages.")
                    .type(GraphQLNonNull(Scalars.GraphQLInt))
                    .build()
            )
            .build()
    }

    /**
     * Called from [io.orangebuffalo.simpleaccounting.infra.graphql.SaSchemaGeneratorHooks.didBuildSchema].
     * Registers the generated edge types as additional types in the schema.
     */
    fun addEdgeTypesToSchema(builder: GraphQLSchema.Builder): GraphQLSchema.Builder {
        pendingEdgeTypes.forEach { builder.additionalType(it) }
        return builder
    }

    private fun getGraphQLName(kClass: KClass<*>): String =
        kClass.findAnnotation<GraphQLName>()?.value
            ?: kClass.simpleName?.removeSuffix("GqlDto")
            ?: error("Cannot determine GraphQL name for ${kClass.qualifiedName}")

    private fun toHumanReadable(pascalCaseName: String): String =
        pascalCaseName.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ").lowercase()

    private fun pluralize(name: String): String = when {
        name.endsWith("y") -> "${name.dropLast(1)}ies"
        name.endsWith("x") -> "${name}es"
        else -> "${name}s"
    }
}
