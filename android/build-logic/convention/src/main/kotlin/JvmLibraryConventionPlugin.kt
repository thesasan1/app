import org.gradle.api.Plugin
import org.gradle.api.Project

/** Pure Kotlin/JVM modules (no Android framework), e.g. :core:common. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureStaticAnalysis()
            configureKotlinJvm()
        }
    }
}
