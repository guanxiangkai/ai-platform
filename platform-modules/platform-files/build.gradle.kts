dependencies {
    implementation(libs.bundles.platform.service)
    implementation(libs.web.plus.web)
    runtimeOnly(libs.postgresql)
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    annotationProcessor(libs.spring.boot.configuration.processor)

    implementation(libs.aws.s3)
    implementation(project(":platform-api:platform-files-api"))
}
