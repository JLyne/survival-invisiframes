import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    java
    alias(libs.plugins.pluginYmlPaper)
}

group = "uk.co.notnull"
version = "2.2-SNAPSHOT"
description = "SurvivalInvisiFrames"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(libs.paperApi)
    compileOnly(libs.customItems)
    compileOnly(libs.creativeItemFilter)
}

paper {
    main = "com.darkender.plugins.survivalinvisiframes.SurvivalInvisiframes"
    apiVersion = libs.versions.paperApi.get().replace(Regex("\\-R\\d.\\d-SNAPSHOT"), "")
    authors = listOf("Jim (AnEnragedPigeon)", "Techdoodle")

    permissions {
        register("survivalinvisiframes.place") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("survivalinvisiframes.craft") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("survivalinvisiframes.cmd") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("survivalinvisiframes.reload") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("survivalinvisiframes.forcerecheck") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("survivalinvisiframes.get") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }

    serverDependencies {
        register("CustomItems") {
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
            required = false
        }
        register("CreativeItemFilter") {
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
            required = false
        }
    }
}

tasks {
    compileJava {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
        options.encoding = "UTF-8"
    }
}
