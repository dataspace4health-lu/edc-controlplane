plugins {
    `java-library`
    id("checkstyle")
    id("application")
    alias(libs.plugins.shadow)
}

version = ""

allprojects {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
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
    implementation(libs.edc.data.plane.selector.api)
    implementation(libs.edc.data.plane.selector.core)
    implementation(libs.edc.data.plane.selector.control.api)
    implementation(libs.edc.control.plane.api.client)
    implementation(libs.edc.control.api.configuration)
    

    // Missing dependencies for injection
    //implementation(libs.edc.iam.mock)
    implementation(libs.edc.edr.store.core)
    implementation(libs.edc.dsp)

    // Logging & JSON-LD
    implementation(libs.edc.monitor.jdk.logger)
    implementation(libs.edc.json.ld.lib)
    implementation(libs.edc.api.observability)
    
    // PostgreSQL support
    implementation(libs.bundles.sql.edc)

    // Vault support
    implementation(libs.edc.vault.hashicorp)
    implementation(libs.edc.sts.remote.client)

    // Identityhub support
    implementation(libs.edc.identity.trust.service)
    implementation(libs.edc.core.spi)
    implementation(libs.edc.oauth2.client)
    implementation(libs.edc.did.core)
    implementation(libs.edc.did.web)
    implementation(libs.edc.identity.trust.core)
    implementation(libs.edc.spi.token)
    implementation(libs.edc.spi.jwt)
    implementation(libs.edc.token.core)
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

sourceSets {
    main {
        java {
            // First, clear default srcDirs by removing them one by one
            setSrcDirs(emptySet<File>())

            // Find all directories named 'src' under 'extensions/**/src'
            val srcFolders = file("extensions").walkTopDown()
                .filter { it.isDirectory && it.name == "src" }
                .toSet()

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
        html.outputLocation.set(file("$buildDir/reports/checkstyle/lint.html"))
    }
}

tasks.test {
    // Change the directory for HTML reports
    reports.html.outputLocation.set(file("$buildDir/test-results/html/test.html"))

    // Change the directory for JUnit XML test results
    reports.junitXml.outputLocation.set(file("$buildDir/test-results/xml/test.xml"))
}