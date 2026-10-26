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
    implementation("io.ktor:ktor-server-netty:3.3.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation("io.ktor:ktor-server-status-pages:3.3.3")
    // Beta
    implementation("ai.koog:koog-agents-additions:1.3.0-beta")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}