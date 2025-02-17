import org.gradle.api.JavaVersion.VERSION_1_7

plugins {
    application
    kotlin("jvm") version "1.7.20-M1"
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

// VERSION: 1.7.20-M1-release-125
kotlin {
    jvmToolchain(8)
}
