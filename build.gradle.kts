plugins {
    `java-library`
    id("checkstyle")
    id("application")
    id("test-report-aggregation")
    id("jacoco")
    alias(libs.plugins.shadow)
}

version = ""

allprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
    }

    // Pin jackson-core (only) to 2.21.1 to fix GHSA-72hv-8253-57qq (Number Length Constraint
    // Bypass in Async Parser, published 2026-02-26). EDC BOM 0.15.1 resolves 2.20.1 which is
    // in the affected range 2.19.0-2.21.0; patched version is 2.21.1.
    // NOTE: only jackson-core received the 2.21.1 security patch — jackson-annotations and
    // jackson-databind have no 2.21.1 release and must remain at their BOM-resolved versions.
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-core") {
                useVersion("2.21.1")
                because("GHSA-72hv-8253-57qq: pin jackson-core to 2.21.1 (patched)")
            }
        }
    }
}

dependencies {
    // Core runtime
    implementation(libs.edc.boot)
    implementation(libs.edc.runtime.core)
    implementation(libs.edc.connector.core)
    implementation(libs.edc.http)
    implementation(libs.jakarta.rsApi)

    // Management + control plane APIs
    implementation(libs.edc.management.api)
    implementation(libs.edc.control.plane.api)
    implementation(libs.edc.control.plane.core)

    // Data plane (HTTP)
    implementation(libs.edc.data.plane.signaling.client)
    implementation(libs.edc.transfer.data.plane.signaling)
    implementation(libs.edc.data.plane.selector.api)
    implementation(libs.edc.data.plane.selector.core)
    implementation(libs.edc.data.plane.selector.control.api)
    implementation(libs.edc.control.plane.api.client)
    implementation(libs.edc.control.api.configuration)
    
    // Missing dependencies for injection
    implementation(libs.edc.edr.store.core)
    implementation(libs.edc.sql.edrcache)
    // CRITICAL: EDR Store Receiver - listens to transfer events and saves EDRs to vault
    implementation(libs.edc.edr.store.receiver)
    implementation(libs.edc.dsp)
    implementation(libs.edc.participant.context.single.core)
    implementation(libs.edc.decentralized.claims.core)
    
    implementation(libs.edc.json.ld.lib)
    implementation(libs.edc.api.observability)
    
    // PostgreSQL support
    implementation(libs.bundles.sql.edc)
    implementation(libs.edc.sql.lease.core)
    implementation(libs.edc.sql.dataplane.instancestore)

    // Vault support
    implementation(libs.edc.vault.hashicorp)
   
    implementation(libs.edc.core.spi)
    implementation(libs.edc.oauth2.client)
    implementation(libs.edc.did.core)
    implementation(libs.edc.did.web)
    implementation(libs.edc.spi.token)
    implementation(libs.edc.spi.jwt)
    implementation(libs.edc.token.core)

    implementation(project(":extensions:scope"))
    implementation(project(":extensions:common:http:client"))
    implementation(project(":extensions:common:decentralized-claims-issuers-configuration"))
    
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

sourceSets {
    main {
        java {
            // First, clear default srcDirs by removing them one by one
            setSrcDirs(emptySet<File>())

            // Find all directories named 'main' under extensions/**/src/main, spi/**/src/main, services/**/src/main
            val srcFolders = listOf("extensions").flatMap { folder ->
                file(folder).walkTopDown()
                    .filter { it.isDirectory && it.name == "main" && it.parent.endsWith("src") }
                    .toList()
            }.toSet()

            // Add those directories as source dirs
            setSrcDirs(srcFolders)
        }
    }
}

checkstyle {
    toolVersion = "12.0.0"
    configFile = file("config/checkstyle/checkstyle.xml")
    isShowViolations = true
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveClassifier.set("")
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(false)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.file("reports/checkstyle/lint.html"))
    }
}

subprojects {
    plugins.apply("java")
    plugins.apply("jacoco")
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // optional per-task report locations; not necessary for aggregation
        
        testLogging {
            events("FAILED", "PASSED", "SKIPPED")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
        }

        reports {
            junitXml.required.set(true)
            junitXml.outputLocation.set(layout.buildDirectory.dir("reports/junit/xml"))
            html.required.set(true)
            html.outputLocation.set(layout.buildDirectory.dir("reports/junit/html"))
        }
        finalizedBy(tasks.jacocoTestReport)
}
jacoco {
        toolVersion = "0.8.10"
}
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/xml/jacoco.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html"))
            csv.required.set(false)
        }
    }
}

// 1) Register a single aggregation task
val allTests by tasks.register<TestReport>("allTests") {
    destinationDirectory.set(layout.buildDirectory.dir("reports/junit/html"))
    // Collect results from all subprojects’ "test" tasks
    testResults.from(subprojects.map { it.tasks.named("test") })
}

// 2) Ensure aggregation runs even if tests fail
// finalizedBy makes Gradle run 'allTests' regardless of the outcome of 'test'
tasks.named("test") {
    finalizedBy(allTests)
}