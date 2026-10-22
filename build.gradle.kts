plugins {
    kotlin("jvm") version "2.4.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Stable
    implementation("ai.koog:http-client-ktor:1.3.0")
    implementation("ai.koog:agents-features-memory:1.3.0")
    // Beta
    implementation("ai.koog:koog-agents-additions:1.3.0-beta")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}