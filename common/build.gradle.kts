import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
import org.jetbrains.compose.compose
import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties

plugins {
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    id("com.android.kotlin.multiplatform.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.codingfeline.buildkonfig")
    id("io.gitlab.arturbosch.detekt")
}

abstract class CopyEnvironmentAssetsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copyAssets() {
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        inputDirectory.get().asFile.copyRecursively(output, overwrite = true)
    }
}

val properties: Properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())
val environment = properties.getProperty("environment")
val prodApiUrl = properties.getProperty("prod.apiUrl")
val prodAuthHeader = properties.getProperty("prod.authHeader")
val testApiUrl = properties.getProperty("test.apiUrl")
val testAuthHeader = properties.getProperty("test.authHeader")

val copyCommonResourcesToRaw = tasks.register<CopyEnvironmentAssetsTask>("copyCommonResourcesToRaw") {
    inputDirectory.set(layout.projectDirectory.dir("src/commonMain/resources/$environment"))
    outputDirectory.set(layout.buildDirectory.dir("generated/featureToggleAssets"))
}

kotlin {
    android {
        namespace = "com.marzec.todo.common"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JVM_17)
        }
        androidResources {
            enable = true
        }
    }
    jvm("desktop") {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        named("commonMain") {
            resources.srcDir("src/commonMain/resources/$environment")
            dependencies {
                api(project(":multiplatformUtils"))
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api(libs.kotlinStdlib)
                api(libs.coroutineCore)
                api(libs.ktorClient)
                api(libs.ktorSerialization)
                api(libs.serializationJson)
                implementation(libs.kotlinDateTime)
                implementation(libs.kotlinImmutable)
                implementation(libs.compose.icons)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutineTest)
            }
        }
        named("androidMain") {
            dependencies {
                api("androidx.appcompat:appcompat:1.1.0")
                api("androidx.core:core-ktx:1.3.1")
                api(libs.ktorOkHttpClient)

                // data store
                api(libs.datastore.preferences)
                api(libs.datastore)

                implementation(libs.compose.viewModel)
            }
        }
        named("desktopMain") {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api(libs.ktorOkHttpClient)
            }
        }
    }
}


androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addStaticSourceDirectory("src/jvmAndAndroidMain/kotlin")
        variant.sources.assets?.addGeneratedSourceDirectory(
            copyCommonResourcesToRaw,
            CopyEnvironmentAssetsTask::outputDirectory
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    compilerOptions {
        jvmTarget.set(JVM_17)
        freeCompilerArgs.addAll(
            "-Xallow-jvm-ir-dependencies",
            "-Xskip-prerelease-check")
    }
}

buildkonfig {
    packageName = "com.marzec.todo"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "ENVIRONMENT", environment)
        buildConfigField(FieldSpec.Type.STRING, "PROD_API_URL", prodApiUrl)
        buildConfigField(FieldSpec.Type.STRING, "PROD_AUTH_HEADER", prodAuthHeader)
        buildConfigField(FieldSpec.Type.STRING, "TEST_API_URL", testApiUrl)
        buildConfigField(FieldSpec.Type.STRING, "TEST_AUTH_HEADER", testAuthHeader)
    }
}

detekt {
    source = files(
        "src/androidMain/kotlin",
        "src/commonMain/kotlin",
        "src/jvmAndAndroidMain/kotlin",
        "src/desktopTest/kotlin",
        "src/desktopMain/kotlin"
    )

    config = files("../config/detekt/detekt.yml")
}
