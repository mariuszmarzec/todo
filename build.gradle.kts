buildscript {

    val kotlinVersion = "1.9.10"
    val androidGradlepluginVersion = "8.1.1"
    val buildkonfigVersion = "0.11.0"
    val detektVersion = "1.18.1"
    val atomicfuVersion = "0.20.2"

    val composePluginVersion = "1.6.1"


    val androidBuildPlugin = "com.android.tools.build:gradle:${androidGradlepluginVersion}"
    val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}"
    val serializationGradlePlugin = "org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion"
    val buildKonfigPlugin =
        "com.codingfeline.buildkonfig:buildkonfig-gradle-plugin:$buildkonfigVersion"
    val detektPlugin = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:$detektVersion"
    val atomicPlugin = "org.jetbrains.kotlinx:atomicfu-gradle-plugin:$atomicfuVersion"
    val composeGradlePlugin = "org.jetbrains.compose:compose-gradle-plugin:$composePluginVersion"

    repositories {
        google()
        jcenter()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    dependencies {
        classpath(androidBuildPlugin)
        classpath(kotlinGradlePlugin)
        classpath(serializationGradlePlugin)
        classpath(composeGradlePlugin)
        classpath(buildKonfigPlugin)
        classpath(detektPlugin)
        classpath(atomicPlugin)
        // TODO wait for stable 7.3 AGP
//        classpath(libs.googleServices)
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        val compilerArgs = options.compilerArgs
        compilerArgs.add("-Xmaxerrs")
        compilerArgs.add("500")
    }
}
