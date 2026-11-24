import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Formatting (Spotless + ktlint) and static analysis (detekt) for every Kotlin module
 * (ADR-016). Run `./gradlew spotlessApply` to fix formatting and `./gradlew detekt` to analyze.
 */
internal fun Project.configureStaticAnalysis() {
    val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
    val detektKotlinVersion = libs.findVersion("detektKotlin").get().requiredVersion

    // The repository-root .editorconfig holds the ktlint settings shared with IDEs.
    val editorConfig = rootProject.file("../.editorconfig")
    pluginManager.apply("com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(ktlintVersion).setEditorConfigPath(editorConfig)
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(ktlintVersion).setEditorConfigPath(editorConfig)
        }
    }

    pluginManager.apply("io.gitlab.arturbosch.detekt")
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        source.setFrom("src/main/kotlin", "src/test/kotlin")
        parallel = true
    }
    tasks.withType<Detekt>().configureEach {
        jvmTarget = "17"
        reports {
            html.required.set(true)
            sarif.required.set(true)
            xml.required.set(false)
            txt.required.set(false)
            md.required.set(false)
        }
    }

    // The project's Kotlin version would otherwise leak onto detekt's own classpath and break it.
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion(detektKotlinVersion)
        }
    }
}
