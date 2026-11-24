plugins {
    alias(libs.plugins.app.android.feature)
}

android {
    namespace = "com.thesasan1.app.feature.notes"
}

dependencies {
    implementation(project(":core:database"))
}
