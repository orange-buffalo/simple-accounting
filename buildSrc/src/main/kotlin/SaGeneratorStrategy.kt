import org.jooq.codegen.DefaultGeneratorStrategy
import org.jooq.codegen.GeneratorStrategy
import org.jooq.meta.*
import org.jooq.tools.StringUtils

class SaGeneratorStrategy : DefaultGeneratorStrategy() {
    override fun getJavaClassName(definition: Definition, mode: GeneratorStrategy.Mode): String =
        if (definition is CatalogDefinition && definition.isDefaultCatalog)
            "DefaultCatalog"
        else if (definition is SchemaDefinition && definition.isDefaultSchema)
            "DefaultSchema"
        else {
            val validName = StringUtils.toCamelCase(
                definition.outputName
                    .replace(' ', '_')
                    .replace('-', '_')
                    .replace('.', '_')
            )
            if (definition is TableDefinition && mode != GeneratorStrategy.Mode.PATH) "${validName}PathTable"
            else if (definition is TableDefinition && mode != GeneratorStrategy.Mode.RECORD) "${validName}Table"
            else if (definition is TableDefinition && mode == GeneratorStrategy.Mode.RECORD) "${validName}Record"
            else validName
        }

    override fun getJavaIdentifier(definition: Definition): String {
        if (definition is ColumnDefinition) {
            return StringUtils.toCamelCaseLC(definition.outputName)
        }
        return super.getJavaIdentifier(definition)
    }
}
