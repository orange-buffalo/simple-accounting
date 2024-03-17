import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files

/**
 * Based on `hotReloadEnabled` in `.test-config.yaml`, switches to JBR in order to support
 * advanced class redefinitions and thus enhanced hot reload. See development docs
 * for the details on how this speeds up development lifecycle.
 */
class SaHotReloadPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val hotReloadEnabled: Boolean by lazy { hotReloadEnabled(target) }

        target.configure<JavaPluginExtension> {
            toolchain {
                if (hotReloadEnabled) {
                    // logging is tricky in Gradle as most of the output is hidden by default,
                    // thus using stdout directly to let user know about the changes
                    println("SaHotReloadPlugin: Be ware! As hot reload is enabled in configs, using JBR")
                    @Suppress("UnstableApiUsage")
                    vendor.set(JvmVendorSpec.JETBRAINS)
                } else {
                    vendor.set(JvmVendorSpec.ADOPTIUM)
                }
            }
        }

        target.tasks.withType<Test> {
            if (hotReloadEnabled) {
                jvmArgs = jvmArgs + "-XX:+AllowEnhancedClassRedefinition"
            }
        }
    }

    private fun hotReloadEnabled(project: Project): Boolean {
        val configFile = project.file("src/test/.test-config.yaml")
        if (configFile.exists()) {
            val yaml = Yaml()
            val inputStream = Files.newInputStream(configFile.toPath())
            val config = yaml.load<Map<String, Any>>(inputStream)
            val hotReloadEnabled = config["hotReloadEnabled"] as Boolean?
            if (hotReloadEnabled == null) {
                println("SaHotReloadPlugin: No hotReloadEnabled property found in test config file at ${configFile.absolutePath}, hot reload is not enabled")
                return false
            } else {
                println("SaHotReloadPlugin: Hot reload is ${if (hotReloadEnabled) "enabled" else "disabled"}")
                return hotReloadEnabled
            }
        } else {
            println("SaHotReloadPlugin: No test config file found at ${configFile.absolutePath}, hot reload is not enabled")
            return false
        }
    }
}
