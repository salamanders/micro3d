plugins {
    kotlin("jvm") version "2.2.10"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"

}

group = "info.benjaminhill"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.java-native:jssc:2.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("org.slf4j:slf4j-simple:2.0.17")
    implementation("org.slf4j:slf4j-api:2.0.17")
    // testImplementation(kotlin("test"))

    // https://mvnrepository.com/artifact/org.bytedeco/javacv
    implementation("org.bytedeco:javacv:1.5.12")
    // https://mvnrepository.com/artifact/org.bytedeco/javacv-platform
    implementation("org.bytedeco:javacv-platform:1.5.12")
    // https://mvnrepository.com/artifact/org.bytedeco/opencv
    implementation("org.bytedeco:opencv:4.11.0-1.5.12")

    // https://mvnrepository.com/artifact/org.openpnp/opencv
    // implementation("org.openpnp:opencv:4.9.0-0")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    //runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.1.10")
    implementation(kotlin("reflect"))

}

application {
    mainClass.set("info.benjaminhill.micro3d.MainKt")
}

tasks.test {
    // useJUnitPlatform()
}