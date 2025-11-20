// anvil-engine/build.gradle.kts
version = "0.1.2"   // ← YES! This is what adds the version

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
    mainClass.set("dev.badkraft.anvil.engine.AuroraEngine")
}

tasks.test {
    // We have no tests yet — don't blow up the build
    //useJUnitPlatform()                      // if you ever add tests later
    failOnNoDiscoveredTests = false
}