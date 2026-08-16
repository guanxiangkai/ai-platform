dependencies {
    api(libs.bundles.http.contract)
    implementation(libs.bundles.http.client.runtime)
    implementation(libs.bundles.platform.api.files)
    implementation(project(":platform-api:platform-core-api"))
}
