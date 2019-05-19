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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm")
    id("net.minecraftforge.gradle.forge")
}

base.archivesBaseName = prop("modBaseName") + "-core-" + prop("mcVersion")

minecraft {
    version = prop("forgeVersion")
    mappings = prop("mcpVersion")
    runDir = "run"
    coreMod = prop("coreMod")
}

tasks {
    getByName<Jar>("jar") {
        manifest {
            attributes(/*
                "FMLCorePlugin" to prop("coreMod"),*/
                "Maven-Artifact" to "${project.group}:${project.base.archivesBaseName}:${project.version}",
                "Timestamp" to System.currentTimeMillis()
            )
        }
        classifier = "deprecated"
    }

    getByName<ProcessResources>("processResources") {
        val props = mapOf(
            "version" to project.version,
            "mcversion" to minecraft.version
        )

        inputs.properties(props)

        from(sourceSets["main"].resources.srcDirs) {
            include("mcmod.info")
            expand(props)
        }

        from(sourceSets["main"].resources.srcDirs) {
            exclude("mcmod.info")
        }
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            javaParameters = true
            freeCompilerArgs += "-Xjvm-default=enable"
        }
    }
}

repositories {
    jcenter()
    mavenCentral()
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)
}

val deobfJar by tasks.creating(Jar::class) {
    from(sourceSets["main"].output)
    manifest {
        attributes(
            "FMLCorePlugin" to prop("coreMod"),
            "Maven-Artifact" to "${project.group}:${project.base.archivesBaseName}:${project.version}",
            "Timestamp" to System.currentTimeMillis()
        )
    }
}

val sourceJar by tasks

parent!!.publishing {
    publications.create("core", MavenPublication::class) {
        from(components["java"])
        artifact(sourceJar)
        artifact(deobfJar)
        this.artifactId = base.archivesBaseName
    }
}

fun hasProp(name: String): Boolean = extra.has(name)

fun prop(name: String): String? = extra.properties[name] as? String
