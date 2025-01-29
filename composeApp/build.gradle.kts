import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.androidx.room.runtime)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor)
            implementation(libs.kotlinx.datetime)
            implementation(libs.composeIcons.featherIcons)
            implementation(libs.sqlite.bundled)
            implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.29.0")
            implementation("com.mikepenz:multiplatform-markdown-renderer-code:0.29.0")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "DeepSeekR1"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("desktopAppIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("desktopAppIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("desktopAppIcons/MacosIcon.icns"))
                bundleID = "com.kashif.deepseek.desktopApp"
            }
        }
    }
}
dependencies {
    add("kspJvm", libs.androidx.room.compiler)

}

room {
    schemaDirectory("$projectDir/schemas")
}