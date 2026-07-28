plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.asset.spi)
    implementation(libs.edc.control.plane.spi)
    implementation(libs.edc.web.spi)
    implementation(libs.edc.http.spi)
    implementation(libs.jakarta.rsApi)
    implementation(libs.apache.pdfbox)
    implementation(libs.jackson.databind)

    // Test dependencies
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    // JAX-RS runtime needed by LegalContractApiControllerTest to instantiate Response objects
    testImplementation("org.glassfish.jersey.core:jersey-server:3.1.11")
    testImplementation("org.glassfish.jersey.inject:jersey-hk2:3.1.11")
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.edc.core.spi)
}
