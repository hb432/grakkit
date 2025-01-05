import org.gradle.internal.os.OperatingSystem

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
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