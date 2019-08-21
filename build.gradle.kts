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

import groovy.util.Node
import groovy.util.NodeList
import net.minecraftforge.gradle.user.TaskSingleReobf
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm")
    kotlin("plugin.noarg")
    id("net.minecraftforge.gradle.forge")
}

allprojects {
    version = prop("modVersion")!!
    group = prop("modGroup")!!
}
base.archivesBaseName = prop("modBaseName") + "-" + prop("mcVersion")

minecraft {
    version = prop("forgeVersion")
    mappings = prop("mcpVersion")
    runDir = "run"

    replace("GRADLE:VERSION", project.version)
    replaceIn("BlueRPG.kt")
}

noArg {
    annotation("com.teamwizardry.librarianlib.features.autoregister.PacketRegister")
}

repositories {
    jcenter()
    mavenCentral()
    maven {
        url = uri("https://maven.bluexin.be/repository/snapshots/")
    }
    maven {
        url = uri("https://maven.bluexin.be/repository/releases/")
    }
//    maven { url "https://kotlin.bintray.com/kotlinx" }
    maven {
        name = "Jitpack.io"
        url = uri("https://jitpack.io")
    }
}

val contained by configurations.creating {
    isTransitive = false
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)
}

val kotlin_version: String by extra
dependencies {
    api(project(":coremod"))
    contained(project(":coremod"))
    implementation("com.saomc:saoui:1.12.2-2.0.0.14-SNAPSHOT:deobf") {
        exclude(group = "com.teamwizardry.librarianlib")
    }

    contained("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version")
    contained("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")

    api("com.teamwizardry.librarianlib:librarianlib-1.12.2:4.20-SNAPSHOT") {
        because("This is more up-to-date")
    }
    implementation("moe.plushie:armourers_workshop-1.12.2:0.49.0-SNAPSHOT:deobf")
    deobfCompile("com.fantasticsource.dynamicstealth:DynamicStealth:1.12.2.078")
    deobfCompile("com.fantasticsource.dynamicstealth:FantasticLib:1.12.2.016")
    api("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
}

tasks {
    getByName<Jar>("jar") {
        classifier = "deprecated"
        manifest {
            attributes(
                "FMLAT" to "bluerpg_at.cfg",
                "Maven-Artifact" to "${project.group}:${project.base.archivesBaseName}:${project.version}",
                "Timestamp" to System.currentTimeMillis()
            )
        }
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

val deobfJar by tasks.creating(Jar::class) {
    from(sourceSets["main"].output)
    manifest {
        attributes("FMLAT" to "bluerpg_at.cfg")
    }
}

val reobfJar by tasks.getting(TaskSingleReobf::class) {
    extraSrgLines += "PK: gnu/jel saoui/shade/gnu/jel"
}

val sourceJar by tasks

evaluationDependsOnChildren()
val bundleJar = tasks.create("bundleJar", Jar::class) {
    classifier = "bundle"
    dependsOn += reobfJar
    from(zipTree(reobfJar.jar))

    from(contained.resolvedConfiguration.resolvedArtifacts.map {
        val r = files(it.file)
        if (!it.file.exists()) r
        else {
            val ft = zipTree(it.file)
            val f = ft.find { it.name == "MANIFEST.MF" }
            if (f == null || "Maven-Artifact" !in f.readText()) {
                val nf = file("$buildDir/tmp/${it.file.name}.meta")
                nf.writeText("Maven-Artifact: $it.moduleVersion")
                r.from(nf)
            }
            r
        }
    }) {
        into("META-INF/libraries")
    }

    manifest {
        attributes(
            "FMLAT" to "bluerpg_at.cfg",
            "ContainedDeps" to contained.files.joinToString(separator = " ") { it.name },
            "Maven-Artifact" to "${project.group}:${project.base.archivesBaseName}:${project.version}",
            "Timestamp" to System.currentTimeMillis()
        )
    }
}

publishing {
    publications.create<MavenPublication>("publication") {
        from(components["java"])
        artifact(reobfJar.jar) {
            builtBy(reobfJar)
            classifier = "release"
        }
        artifact(sourceJar)
        artifact(deobfJar)
        artifact(bundleJar)
        this.artifactId = base.archivesBaseName

        pom {
            withXml {
                val deps = ((asNode()["dependencies"] as NodeList)[0] as Node).value() as NodeList
                val artifactId =
                    deps.asSequence().map { (((it as Node)["artifactId"] as NodeList)[0] as Node).value() as NodeList }
                        .first {
                            it[0] == "coremod"
                        }
                artifactId[0] = project(":coremod").base.archivesBaseName
            }
        }
    }

    repositories {
        val mavenPassword = if (hasProp("local")) null else prop("mavenPassword")
        maven {
            val remoteURL =
                "https://maven.bluexin.be/repository/" + (if ((version as String).contains("SNAPSHOT")) "snapshots" else "releases")
            val localURL = "file://$buildDir/repo"
            url = uri(if (mavenPassword != null) remoteURL else localURL)
            if (mavenPassword != null) {
                credentials(PasswordCredentials::class.java) {
                    username = prop("mavenUser")
                    password = mavenPassword
                }
            }
        }
    }
}

fun hasProp(name: String): Boolean = extra.has(name)

fun prop(name: String): String? = extra.properties[name] as? String
