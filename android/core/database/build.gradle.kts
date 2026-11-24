plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.android.room)
    alias(libs.plugins.app.hilt)
}

android {
    namespace = "com.thesasan1.app.core.database"
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    sourceSets {
        // MigrationTestHelper reads the exported schema JSON files as test assets.
        getByName("test").assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    api(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)

    // DAO and migration tests run on Robolectric (JUnit 4) through the JUnit Platform vintage engine.
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
