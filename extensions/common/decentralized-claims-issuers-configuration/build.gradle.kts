/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    // Core EDC SPI - provides Monitor, ServiceExtension, Config, TypeManager
    api(libs.edc.core.spi)
    
    // Verifiable Credentials SPI - provides TrustedIssuerRegistry
    api(libs.edc.vc.spi)
    
    // JSON processing - for parsing JSON arrays in config
    api(libs.jackson.databind)
    
    // Testing
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.edc.junit)
    testImplementation(libs.edc.core.spi)
}

