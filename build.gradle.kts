import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.fus.internal.isCiBuild
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
    }
}

plugins {
    alias(libs.plugins.gitSemverPlugin)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spring.dependencyManagement) apply false
    alias(libs.plugins.qodana)
}

semver {
    // tags managed by jreleaser
    createReleaseTag = false
}
val ver = semver.version
allprojects {
    group = "io.orangebuffalo.simpleaccounting"
    version = ver
}

subprojects {
    repositories {
        mavenCentral()
    }
    tasks {
        withType<KotlinCompile> {
            compilerOptions {
                freeCompilerArgs.addAll("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
                jvmTarget.set(JvmTarget.fromTarget(Config.JVM_VERSION.toString()))
            }
        }

        withType<Test> {
            useJUnitPlatform()
            beforeTest(KotlinClosure1<TestDescriptor, Any>(project::printTestDescriptionDuringBuild))
            afterTest(KotlinClosure2<TestDescriptor, TestResult, Any>(project::printTestResultDuringBuild))
            testLogging {
                showStandardStreams = true
            }
        }
        
        named("check") {
            dependsOn(rootProject.tasks.named("qodanaScan"))
        }
    }
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        publishing {
            onlyIf { System.getenv("CI") == "true" }
        }
    }
}

tasks.register("qodanaRebaseline") {
    group = "qodana"
    description = "Regenerates the Qodana baseline by running a scan and copying the results"
    
    dependsOn("qodanaScan")
    
    doLast {
        val sourceFile = file("build/reports/qodana/result-allProblems.sarif.json")
        val targetFile = file(".ci/qodana-baseline.sarif.json")
        
        if (!sourceFile.exists()) {
            throw GradleException("Qodana results not found at ${sourceFile.absolutePath}. Make sure qodanaScan completed successfully.")
        }
        
        sourceFile.copyTo(targetFile, overwrite = true)
        println("✓ Baseline updated at ${targetFile.absolutePath}")
        println("  Don't forget to commit the new baseline file!")
    }
}
