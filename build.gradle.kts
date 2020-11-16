/* -------------------------------------------------------------------------------------------------
Update des Gradle-Wrappers
    VERSION=$(/usr/local/bin/gradle --version | grep -C 0 "Gradle " | sed "s/Gradle //"); \
    gradle wrapper --gradle-version "$VERSION"
*/

@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val slf4j_version: String by project
val kotlin_version: String by project
val assertj_version: String by project
val junit5_version: String by project

val kotlinCoroutines_version: String by project

val logback_version: String by project
val joda_version: String by project
val commons_lang_version: String by project

val mybatis_version: String by project
val sqlite_version: String by project
val postgres_version: String by project

val gson_version: String by project

plugins {
    // id("java")
    kotlin("jvm") version "1.4.10"  // Version: https://goo.gl/RriFBZ

    // [ Optional ]
    // Only if you use Artifactory - see below
    //id "com.jfrog.artifactory" version "4.6.2"      // Version: https://goo.gl/WjN2LZ

    // apply plugin: 'maven-publish' // Only if you want to publish to local Maven-Repo

    id("application")
}

// [ Optional ]
// Rename versions.gradle.optional to versions.gradle and include this file as below
// apply from: "versions.gradle"

version = "0.0.1"
group = "at.mikemitterer.template"

java.sourceCompatibility = JavaVersion.VERSION_1_8

// Weitere Infos:
//      https://docs.gradle.org/current/userguide/application_plugin.html
application {
    mainClass.set("at.mikemitterer.template.MainKt")
}

// springBoot {
//     mainClassName = "at.mikemitterer.catshostel.ApplicationKt"
// }

// - SourceSet —————————————————————————————————————————————————————————————————————————————————————

kotlin {
    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src")
        }
        getByName("test") {
            kotlin.srcDirs("test/unit")
        }
    }
}

java {
    sourceSets {
        getByName("main").java.srcDirs("src")
        getByName("test").java.srcDirs("test/unit")
    }
}

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("test/resources")

// - Dependencies ——————————————————————————————————————————————————————————————————————————————————

val developmentOnly: Configuration by configurations.creating
configurations {
    runtimeClasspath {
        extendsFrom(developmentOnly)
    }
}

dependencies {
    // Logging ---------------------------------------------------------------------------------------------------------
    implementation("org.slf4j:slf4j-api:$slf4j_version")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinCoroutines_version")

    //implementation group: 'org.apache.commons', name: 'commons-lang3', version: version_commonsLang3

    // Datenbank -------------------------------------------------------------------------------------------------------
    // implementation("org.mybatis:mybatis:$mybatis_version")
    // implementation( "org.postgresql:postgresql:$postgres_version")
    // implementation( "org.xerial:sqlite-jdbc:$sqlite_version")

    // Eigene libs -----------------------------------------------------------------------------------------------------
    // implementation "at.mikemitterer:webapp.communication:$version_webAppCommunication"

    // Sonstiges -------------------------------------------------------------------------------------------------------
    implementation("joda-time:joda-time:$joda_version")
    implementation("org.apache.commons:commons-lang3:$commons_lang_version")
    implementation("com.google.code.gson:gson:$gson_version")

    // Test ============================================================================================================
    testImplementation(kotlin("test-junit5"))

    // Logging
    testImplementation("ch.qos.logback:logback-classic:$logback_version")

    // AssertJ
    testImplementation("org.assertj:assertj-core:$assertj_version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5_version")

    // Kotlin
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutines_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutines_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinCoroutines_version")
}

repositories {
    // Migration from Maven to JCenter: https://goo.gl/d1zQT8
    jcenter()

    mavenCentral()
    // maven { url = uri("https://repo.spring.io/milestone") }

    // Sollte eigentlich nicht benötigt werden
    // mavenLocal()

    // Artifactory / Nexus 3
    //      URL and Username / Password are defined in
    //          ~/.gradle/gradle.properties
    //      (Must be set on your local machine (e.g. Mac and on your Jenkins-Server)

    // Uncomment this if you use Artifactory!!!
    // maven {
    //    url "${artifactory_url}/artifactory/libs-release-local/"
    //    credentials {
    //        username = "${artifactory_username}"
    //        password = "${artifactory_password}"
    //    }
    // }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks {
    /**
     * Baut die App
     * Alle weiteren Schritte werden von build.jenkins erledigt
     *
     * installDist installiert die App in
     *      .e/build/install/kotlin-template
     *
     * Weitere Infos zum styled output:
     *      https://www.thetopsites.net/article/52493357.shtml
     */
    register("deploy") {
        group = "KotlinApp"
        description = "Erstellt die fertige Applikation..."

        dependsOn(
            "jar",
            "installDist"
            // publishToMavenLocal /*uploadArchives*/
        )

        doLast {
            val jarFile = "${base.archivesBaseName}-${project.version}.jar"

            val message = "\nSuccessfully deployed $jarFile"
            println(message)
        }
    }
}
