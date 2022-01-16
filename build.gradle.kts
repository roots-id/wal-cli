import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // To generate executable jar (because of issue with long classpath on Windows)
    // https://imperceptiblethoughts.com/shadow/introduction/#benefits-of-shadow
    // TODO: Try again woth fat jar https://www.baeldung.com/kotlin/gradle-executable-jar
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("java")

    kotlin("jvm") version "1.6.10"
    application
}

group = "com.rootsid.wal"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    google()
    // Required to resolve com.soywiz.korlibs.krypto:krypto-jvm:2.0.6
    maven("https://plugins.gradle.org/m2/")

    maven {
        url = uri("https://maven.pkg.github.com/input-output-hk/better-parse")
        credentials {
            username = System.getenv("PRISM_SDK_USER")
            password = System.getenv("PRISM_SDK_PASSWORD")
        }
    }

    maven {
        url = uri("https://maven.pkg.github.com/roots-id/wal-library")
        credentials {
            username = System.getenv("ROOTS-ID_USER")
            password = System.getenv("ROOTS-ID_PASSWORD")
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))

    // Fixes a build issue
    implementation("com.soywiz.korlibs.krypto:krypto-jvm:2.0.6")

    implementation("com.rootsid.wal:wal-library:1.0-SNAPSHOT")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")

    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.0")

    implementation("com.github.ajalt.mordant:mordant:2.0.0-beta3")

    implementation("io.iohk.atala:prism-crypto:1.2.0")

    implementation("io.iohk.atala:prism-identity:1.2.0")

    implementation("io.iohk.atala:prism-credentials:1.2.0")

    implementation("io.iohk.atala:prism-api:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    implementation("org.litote.kmongo:kmongo:4.4.0")

    implementation("org.didcommx:didcomm:0.3.0")

    implementation("org.didcommx:peerdid:0.2.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

application {
    mainClass.set("MainKt")
}
