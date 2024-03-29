
val kotlinVersion = "1.9.10"
val ktorVersion = "2.3.7"
val okHttpClientVersion = "4.10.0"
val serializationVersion = "1.6.2"
val coreKtxVersion = "1.3.2"
val appCompatVersion = "1.2.0"
val materialVersion = "1.1.0"
val coroutinesVersion = "1.6.0"
val androidActivityXVersion = "1.3.0-alpha02"
val androidGradlepluginVersion = "8.1.1"
val buildkonfigVersion = "0.11.0"
val datastore_version = "1.0.0-alpha05"
val detektVersion = "1.18.1"
val atomicfuVersion = "0.20.2"
val googleServicesVersion = "4.3.10"
val glideVersion = "1.0.0-alpha.1"
val exoPlayerVersion = "2.18.5"
val vlcVersion = "4.7.0"
val mockkVersion = "1.13.2"

private val composePluginVersion = "1.6.1"
private val dateTimeVersion = "0.4.1"
val composeCompilerVersion = "1.5.11"

object Dependency {

    val composeGradlePlugin = "org.jetbrains.compose:compose-gradle-plugin:$composePluginVersion"
    val serializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion"

    val ktorClient = "io.ktor:ktor-client-core:$ktorVersion"
    val ktorOkHttpClient = "io.ktor:ktor-client-okhttp:$ktorVersion"
    val ktorContentNegotiation = "io.ktor:ktor-client-content-negotiation:$ktorVersion"
    val ktorSerialization = "io.ktor:ktor-serialization-kotlinx-json:$ktorVersion"
    val okHttpClientLogger = "com.squareup.okhttp3:logging-interceptor:$okHttpClientVersion"

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

    val activityCompose = "androidx.activity:activity-compose:$composePluginVersion"

    val kotlinDateTime = "org.jetbrains.kotlinx:kotlinx-datetime:$dateTimeVersion"
    val atomicPlugin = "org.jetbrains.kotlinx:atomicfu-gradle-plugin:$atomicfuVersion"

    val googleServices = "com.google.gms:google-services:$googleServicesVersion"

    val imageLoader = "com.github.bumptech.glide:compose:$glideVersion"
    val exoPlayer = "com.google.android.exoplayer:exoplayer:$exoPlayerVersion"
    val vlc = "uk.co.caprica:vlcj:$vlcVersion"

    val mockk = "io.mockk:mockk-jvm:$mockkVersion"
}

object Config {
    object Android {
        val compileSdkVersion = 34
        val applicationId = "com.marzec.todo"
        val minSdkVersion = 30
        val targetSdkVersion = 34
        val versionCode = 1
        val versionName = "1.0"
    }
}
