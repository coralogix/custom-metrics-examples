plugins {
    kotlin("jvm") version "2.4.0"
    application
    id("com.gradleup.shadow") version "9.4.2"
}

group = "com.coralogix"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // OpenTelemetry BOM - keeps every OTel artifact aligned on a single version.
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.42.1"))

    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-sdk-metrics")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Semantic conventions (ServiceAttributes, etc.). Not in the BOM yet, version it directly.
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.28.0-alpha")

    // SLF4J is needed by the OTel exporter at runtime. Using simple binding so logs go to stderr.
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.coralogix.MainKt")
}

tasks.named<JavaExec>("run") {
    // Surface env vars and stdout interactively when running via `gradle run`.
    standardInput = System.`in`
}
