import org.gradle.api.JavaVersion

/** Single source of truth for SDK levels and JVM target. See docs/DECISIONS.md (D-02). */
object ProjectConfig {
    const val COMPILE_SDK = 36
    const val TARGET_SDK = 36
    const val MIN_SDK = 26
    val JAVA_VERSION = JavaVersion.VERSION_17
    const val JVM_TOOLCHAIN = 21
}
