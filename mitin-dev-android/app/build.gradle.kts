plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}
android {
    namespace = "dev.mitin.demo"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.mitin.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("ru", "en")
    }
    flavorDimensions += "environment"
    productFlavors {
        create("internal") {
            dimension = "environment"
            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"
            val endpoint = providers.gradleProperty("mitinApiBaseUrl").orElse("").get()
            require(endpoint.isEmpty() || endpoint == "https://localhost:8443/") { "Only the explicit local TLS harness origin is allowed" }
            buildConfigField("String", "API_BASE_URL", "\"$endpoint\"")
            resValue("string", "app_name", "MITIN DEV · Тест")
        }
        create("demo") {
            dimension = "environment"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
        }
    }
    buildTypes { release { isMinifyEnabled = false } }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true; warningsAsErrors = false }
    testOptions { animationsDisabled = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
androidComponents { beforeVariants(selector().withBuildType("release")) { it.enable = false } }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3:1.5.0-alpha07")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    "internalImplementation"("com.squareup.okhttp3:okhttp:5.3.2")
    "testInternalImplementation"("com.squareup.okhttp3:mockwebserver:5.3.2")
    "testInternalImplementation"("com.squareup.okhttp3:okhttp-tls:5.3.2")
    implementation("androidx.core:core-splashscreen:1.0.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// The private harness supplies only its PUBLIC CA certificate. No key is embedded.
abstract class PrepareInternalCa : DefaultTask() {
    @get:InputFile abstract val certificate: RegularFileProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @TaskAction fun generate() {
        val pem = certificate.get().asFile.readText()
        require(pem.contains("BEGIN CERTIFICATE") && !pem.contains("PRIVATE KEY"))
        val target = outputDirectory.get().file("raw/mitin_test_ca.pem").asFile
        target.parentFile.mkdirs()
        target.writeText(pem)
    }
}
val prepareInternalCa = tasks.register<PrepareInternalCa>("prepareInternalCa") {
    certificate.set(layout.file(providers.gradleProperty("mitinCaPem").map { file(it) }
        .orElse(provider { file("src/internal/res/raw/unconfigured_ca.pem") })))
    outputDirectory.set(layout.buildDirectory.dir("generated/internalCa"))
}
androidComponents.onVariants(androidComponents.selector().withFlavor("environment" to "internal")) { variant ->
    variant.sources.res?.addGeneratedSourceDirectory(prepareInternalCa) { it.outputDirectory }
}

// Dependency inventory for the isolated OSV audit (no credentials, no build cache).
tasks.register("writeDependencyInventory") {
    doLast {
        val coordinates = configurations.filter { it.isCanBeResolved && it.name.endsWith("RuntimeClasspath") }
            .flatMap { it.incoming.resolutionResult.allComponents }
            .mapNotNull { it.moduleVersion?.let { v -> "${v.group}:${v.name}:${v.version}" } }
            .filterNot { it.startsWith("dev.mitin") || it.startsWith(":") }.distinct().sorted()
        layout.buildDirectory.file("reports/dependency-coordinates.txt").get().asFile.apply { parentFile.mkdirs(); writeText(coordinates.joinToString("\n")) }
    }
}
