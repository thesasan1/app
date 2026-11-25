plugins {
    alias(libs.plugins.app.android.feature)
}

android {
    namespace = "com.thesasan1.app.feature.tasks"
}

dependencies {
    implementation(project(":core:database"))
}
