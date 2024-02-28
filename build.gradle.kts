import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.8.20"
}

group = "minerofmillions"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:1.5.0")
    implementation("com.arkivanov.decompose:decompose:2.2.2")
    implementation("com.arkivanov.decompose:extensions-compose-jetbrains:2.2.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
}

compose.desktop {
    application {
        mainClass = "minerofmillions.serverhost.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "server_host_app"
            packageVersion = "1.0.0"
            includeAllModules = true
        }
    }
}

tasks.register<Copy>("installAndCopy") {
    val createDistributable by tasks.getting
    from(createDistributable.outputs.files)
    into(providers.environmentVariable("HOST_INSTALL_DIR"))
}
