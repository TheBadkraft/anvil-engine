plugins {
    java
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:24.1.0")
}

application {
    mainClass.set("aurora.engine.AuroraEngine")
}

tasks.test {
    failOnNoDiscoveredTests = false
}