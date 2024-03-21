import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    application
}

group = "me.ex4ltado"
version = "0.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {

    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-tree:9.3")

    implementation("org.javassist:javassist:3.28.0-GA")

    // Decompiler
    implementation("org.benf:cfr:0.152")

    implementation("com.github.hervegirod:fxsvgimage:1.0b2")
    //maven { url 'https://jitpack.io' }implementation("com.github.afester.javafx:FranzXaver:0.1")

    implementation("commons-io:commons-io:2.11.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("me.ex4ltado.jareditor.JarEditorKt")
}