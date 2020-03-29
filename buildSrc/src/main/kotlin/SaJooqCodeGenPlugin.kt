import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.jooq.codegen.DefaultGeneratorStrategy
import org.jooq.codegen.GenerationTool
import org.jooq.meta.ColumnDefinition
import org.jooq.meta.Definition
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Target
import org.jooq.tools.StringUtils

/**
 * Workaround for https://github.com/etiennestuder/gradle-jooq-plugin/issues/120
 */
class SaJooqCodeGenPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val jooqModelDir = project.file("build/generated-sources/jooq")

        val jooqCodeGenTask = project.tasks.register("jooqGenerateSources", SaJooqCodeGenTask::class.java) {
            flywayMigrations.from(project.file("src/main/resources/db/migration"))
            jooqModelDirectory.set(jooqModelDir.absoluteFile)
            jooqModelPackage.set("io.orangebuffalo.simpleaccounting.services.persistence.impl.model")
        }

        val compileJava = project.tasks.findByName("compileJava")
            ?: throw IllegalStateException("Java compile task is not found")
        compileJava.dependsOn(jooqCodeGenTask)

        val sourceSets = project.property("sourceSets") as SourceSetContainer
        sourceSets.getByName("main").java.srcDir(jooqModelDir)
    }
}

open class SaJooqCodeGenTask : DefaultTask() {

    @InputFiles
    val flywayMigrations: ConfigurableFileCollection = project.objects.fileCollection()

    @OutputDirectory
    val jooqModelDirectory = project.objects.directoryProperty()

    @Input
    val jooqModelPackage = project.objects.property(String::class.java)

    @TaskAction
    open fun generateJooqModel() {
        val outputDirectory = jooqModelDirectory.get().asFile
        outputDirectory.deleteRecursively()

        val jooqConfig = Configuration()
        jooqConfig.generator = Generator()
            .withDatabase(
                Database()
                    .withName("org.jooq.meta.extensions.ddl.DDLDatabase")
                    .withProperties(
                        Property()
                            .withKey("scripts")
                            .withValue(flywayMigrations.joinToString(",") { "$it/*.sql" }),
                        Property()
                            .withKey("sort")
                            .withValue("flyway")
                    )
            )
            .withStrategy(
                Strategy()
                    .withName(SaGeneratorStrategy::class.java.canonicalName)
            )
            .withTarget(
                Target()
                    .withDirectory(outputDirectory.absolutePath)
                    .withPackageName(jooqModelPackage.get())
            )
        GenerationTool.generate(jooqConfig)
    }
}

class SaGeneratorStrategy : DefaultGeneratorStrategy() {
    override fun getJavaIdentifier(definition: Definition): String {
        if (definition is ColumnDefinition) {
            return StringUtils.toCamelCaseLC(definition.outputName)
        }

        return super.getJavaIdentifier(definition)
    }
}


