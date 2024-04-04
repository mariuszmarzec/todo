buildscript {

    repositories {
        google()
        jcenter()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    dependencies {
        classpath(libs.plugin.androidBuild)
        classpath(libs.plugin.kotlinGradle)
        classpath(libs.plugin.serializationGradle)
        classpath(libs.plugin.composeGradle)
        classpath(libs.plugin.buildKonfig)
        classpath(libs.plugin.detekt)
        classpath(libs.plugin.atomic)
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
