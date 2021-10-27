import org.gradle.api.artifacts.Dependency

val kotlinVersion = "1.5.31"
val ktorVersion = "1.4.3"
val serializationVersion = "1.0.1"
val coreKtxVersion = "1.3.2"
val appCompatVersion = "1.2.0"
val materialVersion = "1.1.0"
val coroutinesVersion = "1.5.2"
val androidActivityXVersion = "1.3.0-alpha02"
val androidGradlepluginVersion = "7.1.0-alpha06"
val buildkonfigVersion = "0.11.0"
val datastore_version = "1.0.0-alpha05"
val detektVersion = "1.18.1"

private val composePluginVersion = "1.0.0-beta3"
private val dateTimeVersion = "0.1.1"

object Dependency {

    val composeGradlePlugin = "org.jetbrains.compose:compose-gradle-plugin:$composePluginVersion"
    val serializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion"

    val ktorClient = "io.ktor:ktor-client-core:$ktorVersion"
    val ktorOkHttpClient = "io.ktor:ktor-client-okhttp:$ktorVersion"
    val ktorSerialization = "io.ktor:ktor-client-serialization-jvm:$ktorVersion"

    val androidBuildPlugin = "com.android.tools.build:gradle:${androidGradlepluginVersion}"
    val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}"
    val serializationGradlePlugin = "org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion"
    val buildKonfigPlugin = "com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:$buildkonfigVersion"
    val detektPlugin = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:$detektVersion"

    val androidActivityX = "androidx.activity:activity-compose:$androidActivityXVersion"

    val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    val coroutineCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
    val coroutineTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion"
    val androidxCoreKtx = "androidx.core:core-ktx:$coreKtxVersion"
    val androidxAppCompat = "androidx.appcompat:appcompat:$appCompatVersion"
    val androidMaterial = "com.google.android.material:material:$materialVersion"

    val kotlinDateTime = "org.jetbrains.kotlinx:kotlinx-datetime:$dateTimeVersion"
}

object Config {
    object Android {
        val compileSdkVersion = 30
        val applicationId = "com.marzec.todo"
        val minSdkVersion = 30
        val targetSdkVersion = 30
        val versionCode = 1
        val versionName = "1.0"
    }
}
