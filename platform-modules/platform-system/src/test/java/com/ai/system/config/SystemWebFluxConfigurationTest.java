package com.ai.system.config;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.BlockingExecutionConfigurer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class SystemWebFluxConfigurationTest {

    @Test
    void blockingControllerMethodsShouldUseVirtualThreads() throws Exception {
        AsyncTaskExecutor executor = SystemWebFluxConfiguration.platformSystemBlockingExecutor();
        SystemWebFluxConfiguration configuration = new SystemWebFluxConfiguration(executor);
        InspectableBlockingExecutionConfigurer configurer = new InspectableBlockingExecutionConfigurer();

        configuration.configureBlockingExecution(configurer);

        assertThat(configurer.executor()).isSameAs(executor).isInstanceOf(VirtualThreadTaskExecutor.class);
        assertThat(executor.submit(() -> Thread.currentThread().isVirtual()).get()).isTrue();
    }

    @Test
    void systemControllersShouldNotWrapBlockingServicesInReactiveReturnTypes() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> reactiveEndpoints = new ArrayList<>();

        for (var beanDefinition : scanner.findCandidateComponents("com.ai.system.controller")) {
            Class<?> controllerType = Class.forName(
                    Objects.requireNonNull(beanDefinition.getBeanClassName(), "控制器类名不能为空"));
            for (Method method : controllerType.getDeclaredMethods()) {
                if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)
                        && Publisher.class.isAssignableFrom(method.getReturnType())) {
                    reactiveEndpoints.add(controllerType.getSimpleName() + "#" + method.getName());
                }
            }
        }

        assertThat(reactiveEndpoints)
                .as("platform-system 的同步持久化控制器必须交由统一阻塞执行边界调度")
                .isEmpty();
    }

    private static final class InspectableBlockingExecutionConfigurer extends BlockingExecutionConfigurer {

        private AsyncTaskExecutor executor() {
            return Objects.requireNonNull(getExecutor(), "阻塞执行器必须完成配置");
        }
    }
}
