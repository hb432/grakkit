import org.gradle.internal.os.OperatingSystem
import org.gradle.api.file.FileVisitDetails

plugins {
    java
    id("com.github.node-gradle.node") version "7.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

// Assuming graalVersion is defined in gradle.properties or elsewhere
val graalVersion: String by project

dependencies {
    // common lib deps
    implementation(project(":kernel"))
    implementation("me.carleslc.Simple-YAML:Simple-Yaml:1.8.4")

    // graal deps
    implementation("org.graalvm.js:js-language:${graalVersion}")
    implementation("org.graalvm.polyglot:polyglot:${graalVersion}")
    implementation("org.graalvm.nativeimage:native-image-base:${graalVersion}")
    implementation("org.graalvm.truffle:truffle-api:${graalVersion}")

//    // Add Graal node js
//    implementation ("org.graalvm.js:js:${graalVersion}")
//    implementation ("org.graalvm.js:js-community:${graalVersion}")
//    implementation ("org.graalvm.js:js-scriptengine:${graalVersion}")

    // add javet
    val os = OperatingSystem.current()
    val arch = System.getProperty("os.arch")
    val isI18n = false
    val isNode = false
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
    // log the javet node library loaded
    println("javet-node lib: javet-$jsRuntimeTimeType-$osType-$archType$i18nType:4.1.1")

//    // graal sdk
//    implementation ("org.graalvm.sdk:graal-sdk:${graalVersion}")

    // platform dependencies
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

val targetJavaVersion = 21

val jsProjectDir = file("${project.projectDir}/../../js/grakkit")
// log the jsProjectDir
println("jsProjectDir: ${jsProjectDir}")

////compile js
//tasks.register<com.github.gradle.node.npm.task.NpxTask>("tsc") {
//    dependsOn(tasks.npmInstall)
//    workingDir.set(jsProjectDir)
//    command.set("tsc")
//}

// build the js project
tasks.register<com.github.gradle.node.npm.task.NpxTask>("npmBuild") {
    dependsOn(tasks.npmInstall)
    workingDir.set(jsProjectDir)
    command.set("npm")
    args.set(listOf("run", "build"))
}

// copy package*.json files into dist
tasks.register<Copy>("copyPackageJsonIntoDist") {
    dependsOn("npmBuild")
    from(jsProjectDir)
    into("${jsProjectDir}/dist")
    include("package*.json")
}

//"npm ci --production"
tasks.register<com.github.gradle.node.npm.task.NpxTask>("npmPostBuild") {
    dependsOn("copyPackageJsonIntoDist")
    workingDir.set(jsProjectDir)
    command.set("npm")
    args.set(listOf("ci", "--production"))
}

tasks.register<Copy>("copyJsDistIntoNodeModules") {
    dependsOn("npmPostBuild")
    from(jsProjectDir)
    into("${buildDir}/resources/main/js")
}

tasks.register("generateJsFileList") {
    dependsOn(tasks.named("copyJsDistIntoNodeModules"))
    val jsResourcesDir = file("${buildDir}/resources/main/js")
    val jsFileListFile = file("${buildDir}/resources/main/js-file-list.txt")
    // wipe old jsFile for new writing
    jsFileListFile.delete()

    inputs.dir(jsResourcesDir)
    outputs.file(jsFileListFile)

    doLast {
        jsFileListFile.parentFile.mkdirs()
        jsFileListFile.createNewFile()

        fileTree(jsResourcesDir).visit {
            if (file.isFile) { // Using receiver context, `isFile` is directly accessible
                val relativePath = relativePath.pathString
                jsFileListFile.appendText("$relativePath\n")
            }
        }
    }
}


tasks.processResources {
    dependsOn("generateJsFileList")
}

tasks.jar {
    archiveBaseName.set("grakkit")
    archiveClassifier.set("paper")
    archiveVersion.set(version.toString())
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveBaseName.set("grakkit-paper")
    archiveClassifier.set("paper")
    archiveFileName.set("grakkit-paper.jar")
    from(tasks.processResources.get().outputs) // Include processed resources
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.register("buildJars") { dependsOn(tasks.shadowJar) }