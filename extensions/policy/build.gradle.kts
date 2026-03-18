plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.core.spi)
    implementation(libs.edc.participant.spi)
    implementation(libs.edc.catalog.spi)
    implementation(libs.edc.policy.engine.spi)
    implementation(libs.edc.vc.spi)

    // Test dependencies
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.edc.core.spi)
}


