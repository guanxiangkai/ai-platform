dependencies {
    implementation(libs.bundles.platform.files)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.bundles.postgresql.integration.testing)
    annotationProcessor(libs.spring.boot.configuration.processor)

    implementation(project(":platform-api:platform-files-api"))
}

tasks.withType<Test>().configureEach {
    systemProperty(
        "platformFilesBaseline",
        rootProject.file("deploy/database/V001__create_platform_schema.sql").absolutePath
    )
}
