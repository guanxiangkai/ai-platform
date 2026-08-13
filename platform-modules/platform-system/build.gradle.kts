dependencies {

    implementation(libs.web.plus.core)
    implementation(libs.web.plus.dict)
    implementation(libs.web.plus.error)
    implementation(libs.web.plus.log)
    implementation(libs.web.plus.mq)
    implementation(libs.web.plus.security)
    implementation(libs.web.plus.web)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.starter.webflux.ui)
    implementation(libs.mapstruct.plus.spring.boot.starter)
    implementation(libs.jpa.plus.starter)
    implementation(libs.redis.plus.starter)
    implementation(libs.caffeine)
    implementation(libs.pinyin4j)
    annotationProcessor(libs.spring.boot.configuration.processor)
    runtimeOnly(libs.postgresql)

    // ===== 跨服务 API 契约（HTTP Interface 客户端）=====
    implementation(project(":platform-api:platform-core-api"))
    implementation(project(":platform-api:platform-system-api"))
}
