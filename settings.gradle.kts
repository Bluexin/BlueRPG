/*
 * Copyright (C) 2019.  Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

enableFeaturePreview("STABLE_PUBLISHING")
include("coremod")

val kotlin_version: String by settings
val forgegradle_version: String by settings

pluginManagement {
    repositories {
        maven {
            name = "forge"
            url = uri("https://files.minecraftforge.net/maven")
        }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("org.jetbrains.kotlin")) useVersion(kotlin_version)
            else when (requested.id.id) {
                "net.minecraftforge.gradle.forge" -> useModule("net.minecraftforge.gradle:ForgeGradle:$forgegradle_version")
            }
        }
    }
}
