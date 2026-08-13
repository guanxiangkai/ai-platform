package com.ai.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 平台通用调度控制面启动入口。 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SchedulerApplication {

    /** 启动平台通用调度控制面。 */
    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
