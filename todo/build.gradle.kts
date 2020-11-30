plugins {
    id("java-library")
    id("kotlin")
}

java {
    setSourceCompatibility(JavaVersion.VERSION_1_8)
    setTargetCompatibility(JavaVersion.VERSION_1_8)
}

dependencies {
    implementation(Dependency.kotlinStdlib)
}