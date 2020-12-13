buildscript {

    repositories {
        google()
        jcenter()

    }
    dependencies {
        classpath(Dependency.androidBuildPlugin)
        classpath(Dependency.kotlinGradlePlugin)
        classpath(Dependency.serializationGradlePlugin)
    }
}

allprojects {
    repositories {
        google()
        jcenter()

    }
}

gradle.projectsEvaluated {
    tasks.withType<JavaCompile> {
        val compilerArgs = options.compilerArgs
        compilerArgs.add("-Xmaxerrs")
        compilerArgs.add("500")
    }
}

tasks.create<Delete>("clean") {
    delete = setOf(
            rootProject.buildDir
    )
}