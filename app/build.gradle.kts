plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("jacoco")
    id("io.gitlab.arturbosch.detekt")
}

import java.io.FileInputStream
import java.util.Properties

jacoco {
    toolVersion = "0.8.11"
}

// Carga de credenciales de firma desde keystore.properties (no versionado)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.opoleyes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.opoleyes"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

tasks.withType<Test> {
    // 4 forks × 1280m = 5GB heap ceiling, safe on 12GB RAM.
    // Match CPU cores (4) to avoid I/O contention from Robolectric forks
    // each loading Android framework + data.json (4.5MB) from assets.
    maxParallelForks = 4
    forkEvery = 0
    maxHeapSize = "1280m"
    jvmArgs("-XX:+UseParallelGC", "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1")
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*", "sun.*")
        isEnabled = true
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest", "connectedDebugAndroidTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val fileFilter = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test.*", "androidx/**", "**/*\$Lambda$*", "**/*\$inlined$*"
    )
    val debugTree = fileTree("${project.layout.buildDirectory.get()}/intermediates/javac/debug") {
        excludes += fileFilter
    }
    val kotlinTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        excludes += fileFilter
    }
    classDirectories.setFrom(listOf(debugTree, kotlinTree))
    sourceDirectories.setFrom(listOf("src/main/java", "src/main/kotlin"))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get()) {
        include("jacoco/testDebugUnitTest.exec")
        include("outputs/unit_test_code_coverage/debugUnitTest/*.exec")
        include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
    })
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // SplashScreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Lottie
    implementation("com.github.LottieFiles:dotlottie-android:0.15.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.12.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.datastore:datastore-preferences:1.0.0")
    testImplementation("com.google.code.gson:gson:2.10.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")

    // Instrumented testing (androidTest)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:core:1.5.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt.yml")
    parallel = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
}
