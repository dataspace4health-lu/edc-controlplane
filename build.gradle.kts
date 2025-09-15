plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

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
    implementation(libs.edc.data.plane.core)
    implementation(libs.edc.data.plane.http)
    implementation(libs.edc.data.plane.signaling.api)
    implementation(libs.edc.data.plane.signaling.client)
    implementation(libs.edc.data.plane.selector.api)
    implementation(libs.edc.data.plane.selector.core)
    implementation(libs.edc.control.plane.api.client)

    // Missing dependencies for injection
    implementation(libs.edc.iam.mock)
    implementation(libs.edc.edr.store.core)
    implementation(libs.edc.configuration.filesystem)
    implementation(libs.edc.dsp)
    implementation(libs.edc.control.api.configuration)

    // Logging & JSON-LD
    implementation(libs.edc.monitor.jdk.logger)
    implementation(libs.edc.json.ld.lib)
    implementation(libs.edc.api.observability)
    
    // PostgreSQL support
    implementation(libs.bundles.sql.edc)

}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("connector.jar")
}