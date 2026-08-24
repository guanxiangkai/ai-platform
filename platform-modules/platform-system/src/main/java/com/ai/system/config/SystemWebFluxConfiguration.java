package com.ai.system.config;

import io.github.guanxiangkai.web.plus.core.config.ContextPropagationAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.web.reactive.config.BlockingExecutionConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * 系统服务 WebFlux 阻塞调用边界。
 *
 * <p>系统服务使用同步 JPA、密码计算和 Redis 客户端。Spring WebFlux 会按默认规则识别
 * 非响应式控制器方法，并在此处提供的虚拟线程执行器上调用，避免占用 Netty event-loop。
 * 返回响应式类型的控制器方法必须保持非阻塞，不能用响应式外观包裹同步调用。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class SystemWebFluxConfiguration implements WebFluxConfigurer {

    static final String BLOCKING_EXECUTOR_BEAN_NAME = "platformSystemBlockingExecutor";
    private static final String BLOCKING_THREAD_PREFIX = "platform-system-http-";

    private final AsyncTaskExecutor blockingExecutor;

    public SystemWebFluxConfiguration(
            @Qualifier(BLOCKING_EXECUTOR_BEAN_NAME) AsyncTaskExecutor blockingExecutor
    ) {
        this.blockingExecutor = blockingExecutor;
    }

    /** 创建按请求分配虚拟线程的阻塞控制器执行器。 */
    @Bean(BLOCKING_EXECUTOR_BEAN_NAME)
    public static AsyncTaskExecutor platformSystemBlockingExecutor(
            @Qualifier(ContextPropagationAutoConfiguration.TASK_DECORATOR_BEAN_NAME)
            TaskDecorator taskDecorator) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(BLOCKING_THREAD_PREFIX);
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(taskDecorator);
        return executor;
    }

    @Override
    public void configureBlockingExecution(BlockingExecutionConfigurer configurer) {
        configurer.setExecutor(blockingExecutor);
    }
}
