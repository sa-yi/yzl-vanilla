plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
}

group = "top.tbpdt"
version = "2.0.3"

repositories {
    if (System.getenv("CI")?.toBoolean() != true) {
        maven("https://maven.aliyun.com/repository/public")
    }
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}

mirai {
    noTestCore = true
    setupConsoleTestRuntime {
        classpath = classpath.filter {
            !it.nameWithoutExtension.startsWith("mirai-core-jvm")
        }
    }
}

dependencies {
    val overflowVersion = "2.16.0+"
    compileOnly("top.mrxiaom:overflow-core-api:$overflowVersion")
    testConsoleRuntime("top.mrxiaom:overflow-core:$overflowVersion")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("junit:junit:4.13.2")
}