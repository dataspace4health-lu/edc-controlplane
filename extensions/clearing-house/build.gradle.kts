plugins {
    `java-library`
}

dependencies {
    // Core EDC SPIs
    implementation(libs.edc.core.spi)
    implementation(libs.edc.contract.spi)
    implementation(libs.edc.transfer.spi)
    implementation(libs.edc.http.spi)
    // Store SPIs for looking up full negotiation/transfer objects
    implementation(libs.edc.control.plane.spi)

    // Test dependencies
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
}
