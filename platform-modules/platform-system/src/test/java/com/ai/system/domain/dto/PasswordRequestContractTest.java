package com.ai.system.domain.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordRequestContractTest {

    private static final String PASSWORD = "a".repeat(40);
    private static final String NEW_PASSWORD = "b".repeat(40);

    @ParameterizedTest
    @MethodSource("passwordRequests")
    void shouldBindPasswordFieldsValidateAndKeepThemOutOfResponsesAndLogs(
            Class<?> type, Map<String, Object> fields, Map<String, String> passwords) throws Exception {
        var mapper = JsonMapper.builder().build();
        Object request = mapper.readValue(mapper.writeValueAsString(fields), type);
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            assertThat(validators.getValidator().validate(request)).isEmpty();
        }
        var output = mapper.readTree(mapper.writeValueAsString(request));
        for (var password : passwords.entrySet()) {
            assertThat(type.getMethod(password.getKey()).invoke(request)).isEqualTo(password.getValue());
            assertThat(output.has(password.getKey())).isFalse();
            assertThat(request.toString()).doesNotContain(password.getValue());
        }
    }

    private static Stream<Arguments> passwordRequests() {
        return Stream.of(
                Arguments.of(RegisterCreateDTO.class,
                        Map.of("realName", "测试用户", "deptId", "dept-1", "password", PASSWORD),
                        Map.of("password", PASSWORD)),
                Arguments.of(UserCreateDTO.class,
                        Map.of("username", "test-user", "password", PASSWORD), Map.of("password", PASSWORD)),
                Arguments.of(UserDTO.class,
                        Map.of("id", "user-1", "password", PASSWORD), Map.of("password", PASSWORD)),
                Arguments.of(ChangePasswordRequest.class,
                        Map.of("oldPassword", PASSWORD, "newPassword", NEW_PASSWORD),
                        Map.of("oldPassword", PASSWORD, "newPassword", NEW_PASSWORD)),
                Arguments.of(ResetPasswordRequest.class,
                        Map.of("newPassword", NEW_PASSWORD), Map.of("newPassword", NEW_PASSWORD))
        );
    }
}
