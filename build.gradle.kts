import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
    }
}

repositories {
    jcenter()
}

plugins {
    kotlin("jvm") version "1.3.72"
}

apply(plugin = "kotlin")

version = "0.0.1"

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.5")

    implementation("io.ktor:ktor-client-apache:1.3.1")
    implementation("io.ktor:ktor-client-jackson:1.3.1")
    implementation("io.ktor:ktor-client-auth-jvm:1.3.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.1")
}

tasks.withType(KotlinCompile::class.java).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val fatJar = task("fatJar", type = Jar::class) {
    manifest { attributes["Main-Class"] = "com.henrycourse.AppKt" }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "fatJar" {
        dependsOn(build)
    }
}
