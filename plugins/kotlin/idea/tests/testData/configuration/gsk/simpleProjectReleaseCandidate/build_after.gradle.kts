import org.gradle.api.JavaVersion.VERSION_1_7

plugins {
    application
    kotlin("jvm") version "1.5.20-RC"
}

application {
    mainClassName = "samples.HelloWorld"
}

repositories {
    jcenter()
}

dependencies {
    testCompile("junit:junit:4.12")
    implementation(kotlin("stdlib-jdk8"))
}

// VERSION: 1.5.20-RC-release-26
kotlin {
    jvmToolchain(8)
}
