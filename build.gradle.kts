plugins {
    kotlin("jvm") version "2.1.0"
}

group = "info.benjaminhill"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.java-native:jssc:2.9.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    // https://mvnrepository.com/artifact/org.openpnp/opencv
    implementation("org.openpnp:opencv:4.9.0-0")

    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.slf4j:slf4j-api:1.7.36")
    // testImplementation(kotlin("test"))

    // for Point3D
    // https://mvnrepository.com/artifact/org.openjfx/javafx
    implementation("org.openjfx:javafx:23.0.2")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    //runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.1.10")
    implementation(kotlin("reflect"))

}

tasks.test {
    // useJUnitPlatform()
}