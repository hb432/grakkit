import org.gradle.internal.os.OperatingSystem

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

// Assuming graalVersion is defined in gradle.properties or elsewhere
val graalVersion: String by project

dependencies {
    // common lib deps: https://github.com/Carleslc/Simple-YAML
    implementation("me.carleslc.Simple-YAML:Simple-Yaml:1.8.4")

    // Add graal dependencies
    implementation("org.graalvm.js:js-language:${graalVersion}")
    implementation("org.graalvm.polyglot:polyglot:${graalVersion}")
    implementation("org.graalvm.nativeimage:native-image-base:${graalVersion}")
    implementation("org.graalvm.truffle:truffle-api:${graalVersion}")

//    // Add Graal node js
//    implementation ("org.graalvm.js:js:${graalVersion}")
//    implementation ("org.graalvm.js:js-community:${graalVersion}")
//    implementation ("org.graalvm.js:js-scriptengine:${graalVersion}")
//    // add graal sdk
//    implementation ("org.graalvm.sdk:graal-sdk:${graalVersion}")

    // add javet
    // add javet
    val os = OperatingSystem.current()
    val arch = System.getProperty("os.arch")
    val isI18n = false
    val isNode = true
    val i18nType = if (isI18n) "-i18n" else ""
    val jsRuntimeTimeType = if (isNode) "node" else "v8"
    val osType = if (os.isWindows) "windows" else
        if (os.isMacOsX) "macos" else
            if (os.isLinux) "linux" else ""
    val archType = if (arch == "aarch64" || arch == "arm64") "arm64" else "x86_64"
    implementation("com.caoccao.javet:javet:4.1.1")
    implementation("com.caoccao.javet:javet-$jsRuntimeTimeType-$osType-$archType$i18nType:4.1.1")
    implementation("com.caoccao.javet.buddy:javet-buddy:0.4.0")
    implementation("net.bytebuddy:byte-buddy:1.15.5")
    // log the javet-node lib
    println("javet-node lib: javet-$jsRuntimeTimeType-$osType-$archType$i18nType:4.1.1")

    // npm dependencies
    implementation("org.orienteer.jnpm:jnpm:1.5")
}

tasks.jar {
    archiveBaseName.set("grakkit")
    archiveClassifier.set("kernel")
}

tasks.shadowJar {
    mergeServiceFiles()
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}