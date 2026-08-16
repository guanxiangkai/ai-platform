dependencies {
    api(libs.bundles.http.contract)
    implementation(libs.bundles.http.client.runtime)
    implementation(libs.bundles.platform.api.agent)
    implementation(project(":platform-api:platform-core-api"))
}
