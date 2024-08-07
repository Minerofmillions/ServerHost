import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.8.20"
    id("co.uzzu.dotenv.gradle") version "4.0.0"
}

group = "io.github.minerofmillions"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven {
        url = uri("https://maven.pkg.github.com/Minerofmillions/decompose-utilities")
        name = "GithubPackages"
        credentials {
            username = env.USERNAME.orNull() ?: System.getenv("USERNAME")
            password = env.PACKAGES_TOKEN.orNull() ?: System.getenv("PACKAGES_TOKEN")
        }
    }
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:1.5.0")
    implementation("com.arkivanov.decompose:decompose:3.1.0")
    implementation("com.arkivanov.decompose:extensions-compose:3.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("io.github.minerofmillions:decompose-utilities:1.0.1")
}

compose.desktop {
    application {
        mainClass = "io.github.minerofmillions.serverhost.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "server_host_app"
            packageVersion = "1.0.0"
            includeAllModules = true
        }
    }
}

val installAndCopy by tasks.registering(Copy::class) {
    val createRuntimeDistributable by tasks.getting
    from(createRuntimeDistributable.outputs.files)
    into(providers.environmentVariable("HOST_INSTALL_DIR"))
}
