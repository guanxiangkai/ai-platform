package com.ai.files;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 平台文件服务启动入口。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class FilesApplication {

    /**
     * 启动平台文件服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FilesApplication.class, args);
    }
}
