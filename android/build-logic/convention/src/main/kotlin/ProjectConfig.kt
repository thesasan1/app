import org.gradle.api.JavaVersion

/** Single source of truth for SDK levels and JVM target. See docs/DECISIONS.md (D-02). */
object ProjectConfig {
    const val COMPILE_SDK = 37

    /** Compose 1.13 needs the 37.1+ platform; 37.2 is the current stable minor release. */
    const val COMPILE_SDK_MINOR = 2
    const val TARGET_SDK = 36
    const val MIN_SDK = 26
    val JAVA_VERSION = JavaVersion.VERSION_17
    const val JVM_TOOLCHAIN = 21
}
