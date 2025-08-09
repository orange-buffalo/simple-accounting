import com.netflix.graphql.dgs.codegen.gradle.ClientUtilsConventions
import com.netflix.graphql.dgs.codegen.gradle.CodegenPlugin
import com.netflix.graphql.dgs.codegen.gradle.CodegenPluginExtension
import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.util.GradleVersion

/**
 * Workaround for https://github.com/Netflix/dgs-codegen/issues/859.
 * Same as https://github.com/Netflix/dgs-codegen/blob/master/graphql-dgs-codegen-gradle/src/main/kotlin/com/netflix/graphql/dgs/codegen/gradle/CodegenPlugin.kt,
 * but with `test` source set attached.
 */
class SaDgsCodegenPlugin : Plugin<Project> {
    companion object {
        const val GRADLE_GROUP = "DGS GraphQL Codegen"
        private val logger = Logging.getLogger(CodegenPlugin::class.java)
    }

    override fun apply(project: Project) {
        val codegenExtension = project.extensions.create("codegen", CodegenPluginExtension::class.java)

        project.plugins.apply(JavaPlugin::class.java)

        val generateJavaTaskProvider = project.tasks.register("generateJava", GenerateJavaTask::class.java)
        generateJavaTaskProvider.configure { group = GRADLE_GROUP }

        val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)

        val sourceSets =
            if (GradleVersion.current() >=
                GradleVersion.version("7.1")
            ) {
                javaExtension.sourceSets
            } else {
                throw RuntimeException("Gradle versions < 7.1 are no longer supported by DGS Codegen. Please upgrade your Gradle version.")
            }
        val testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
        val outputDir = generateJavaTaskProvider.map(GenerateJavaTask::getOutputDir)
        testSourceSet.java.srcDirs(project.files(outputDir).builtBy(generateJavaTaskProvider))

        project.configurations.create("dgsCodegen")
        project.configurations.findByName("dgsCodegen")?.isCanBeResolved = true

        addDependencyLock(project, codegenExtension)
    }

    private fun addDependencyLock(
        project: Project,
        codegenExtension: CodegenPluginExtension,
    ) {
        val dependencyLockString = ClientUtilsConventions.getDependencyString()
        try {
            if (codegenExtension.clientCoreConventionsEnabled.getOrElse(true)) {
                project.dependencyLocking.ignoredDependencies.add(dependencyLockString)
                logger.info(
                    "DGS CodeGen added ignored dependency [{}].",
                    dependencyLockString,
                )
            }
        } catch (e: Exception) {
            // do nothing, this is supplemental and seems to work in certain contexts but not in others
            logger.info(
                "Failed to add DGS CodeGen to ignoredDependencies because: {}",
                dependencyLockString,
                e,
            )
        }
    }
}
