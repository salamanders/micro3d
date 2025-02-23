plugins {
    kotlin("jvm") version "2.1.0"
}

group = "info.benjaminhill"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fazecast:jSerialComm:2.11.0")
    implementation("io.github.java-native:jssc:2.9.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.slf4j:slf4j-api:1.7.36")
    // testImplementation(kotlin("test"))
}

tasks.test {
    // useJUnitPlatform()
}