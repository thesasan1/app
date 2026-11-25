plugins {
    alias(libs.plugins.app.android.feature)
}

android {
    namespace = "com.thesasan1.app.feature.habits"
}

dependencies {
    implementation(project(":core:database"))
}
