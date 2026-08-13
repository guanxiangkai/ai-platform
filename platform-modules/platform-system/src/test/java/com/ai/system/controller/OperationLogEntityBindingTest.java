package com.ai.system.controller;

import com.ai.api.system.log.PlatformOperationLog;
import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperationLogEntityBindingTest {

    @Test
    void everyDeclaredOperationLogUsesTheSharedEventEntity() {
        List<Class<?>> controllers = List.of(
                DeptController.class,
                DictController.class,
                DictItemController.class,
                MenuController.class,
                MessageController.class,
                PostController.class,
                RegisterController.class,
                RoleController.class,
                SettingController.class,
                TenantController.class,
                UserController.class
        );

        List<Method> operationMethods = controllers.stream()
                .flatMap(controller -> List.of(controller.getDeclaredMethods()).stream())
                .filter(method -> method.isAnnotationPresent(OperationLog.class))
                .toList();

        assertThat(operationMethods).isNotEmpty();
        assertThat(operationMethods)
                .allSatisfy(method -> assertThat(method.getAnnotation(OperationLog.class).entity())
                        .as("%s#%s", method.getDeclaringClass().getSimpleName(), method.getName())
                        .isEqualTo(PlatformOperationLog.class));
    }
}
