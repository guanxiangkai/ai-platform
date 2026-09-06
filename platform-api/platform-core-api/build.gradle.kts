// 平台跨服务基础契约；依赖由 platform-api 父构建统一声明。
dependencies {
    // ProtocolPasswordEncoder 是公开契约实现，显式导出其 Spring Security API。
    api(libs.spring.security.crypto)
}
