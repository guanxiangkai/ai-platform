dependencies {
    implementation(libs.bundles.platform.service)
    implementation(libs.web.plus.log)
    implementation(libs.spring.boot.starter.webclient)
    implementation(libs.spring.boot.starter.validation)
    runtimeOnly(libs.postgresql)
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    implementation(project(":platform-api:platform-agent-api"))
    annotationProcessor(libs.spring.boot.configuration.processor)
}
