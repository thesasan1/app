import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        compileSdk = ProjectConfig.COMPILE_SDK
        defaultConfig {
            minSdk = ProjectConfig.MIN_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = ProjectConfig.JAVA_VERSION
            targetCompatibility = ProjectConfig.JAVA_VERSION
        }
        lint {
            abortOnError = true
            warningsAsErrors = false
            checkDependencies = true
            xmlReport = true
            htmlReport = true
        }
        testOptions {
            unitTests.isReturnDefaultValues = true
        }
    }
    extensions.configure(KotlinAndroidProjectExtension::class.java) {
        jvmToolchain(ProjectConfig.JVM_TOOLCHAIN)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(false)
        }
    }
    configureUnitTests()
}

internal fun Project.configureKotlinJvm() {
    extensions.configure(KotlinJvmProjectExtension::class.java) {
        jvmToolchain(ProjectConfig.JVM_TOOLCHAIN)
    }
    configureUnitTests()
}

/** JUnit 5 for all local unit tests, plus coroutine test helpers. */
private fun Project.configureUnitTests() {
    dependencies {
        add("testImplementation", platform(libs.findLibrary("junit-bom").get()))
        add("testImplementation", libs.findLibrary("junit-jupiter").get())
        add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
        add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        add("testImplementation", libs.findLibrary("turbine").get())
        add("testImplementation", libs.findLibrary("mockk").get())
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
