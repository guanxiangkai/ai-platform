dependencies {
    implementation(libs.bundles.platform.agent)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.bundles.postgresql.integration.testing)
    implementation(project(":platform-api:platform-agent-api"))
    annotationProcessor(libs.spring.boot.configuration.processor)
}
