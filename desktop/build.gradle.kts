plugins {
    id("java-library")
    id("kotlin")
    kotlin("plugin.serialization")
}

java {
    setSourceCompatibility(JavaVersion.VERSION_1_8)
    setTargetCompatibility(JavaVersion.VERSION_1_8)
}

dependencies {
    implementation(project(":todo"))
    implementation(Dependency.kotlinStdlib)
    implementation(Dependency.ktorClient)
    implementation(Dependency.ktorOkHttpClient)
    implementation(Dependency.ktorSerialization)
    implementation(Dependency.serializationJson)
}