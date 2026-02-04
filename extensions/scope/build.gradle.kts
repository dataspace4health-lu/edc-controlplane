plugins {
    `java-library`
}

dependencies {
    implementation(libs.edc.spi.transform)
    implementation(libs.edc.catalog.spi)
    implementation(libs.edc.spi.identity.did)
    implementation(libs.edc.lib.jws2020)
    implementation(libs.edc.lib.transform)
    implementation(libs.edc.policy.engine.spi)
    implementation(libs.edc.policy.request.context.spi)
    implementation(libs.edc.vc.spi)
    // implementation(libs.edc.dc.issuer.config)
    
    // Test dependencies
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.edc.core.spi)
}


