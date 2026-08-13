package com.ai.system.consumer.log;

import com.ai.api.context.TenantExecutionScope;
import io.github.guanxiangkai.web.plus.log.entity.BaseLog;
import io.github.guanxiangkai.web.plus.log.spi.OssLogHandler;
import com.ai.system.domain.entity.OssLog;
import com.ai.system.service.IOssLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OSS 文件上传日志 SPI 实现
 * <p>
 * Web Plus 2026.13.0 中 {@code UploadLogHandler} 已更名为 {@code OssLogHandler}，
 * 接口签名从 {@code handle(UploadLogRecord)} 变更为 {@code handle(BaseLog)}。
 * <br/>
 * 触发时机：
 * <ol>
 *   <li>Web Plus {@code FileService} 上传/下载完成后自动调用</li>
 *   <li>{@code @OssLog} 注解标注的自定义方法（通过 {@code OssLogAspect} 拦截）</li>
 * </ol>
 * 实体类需配置 {@code Web Plus.file.log-entity-class=com.ai.system.domain.entity.OssLog}。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssLogHandlerImpl implements OssLogHandler {

    private final IOssLogService ossLogService;

    @Async
    @Override
    public void handle(BaseLog baseLog) {
        if (!(baseLog instanceof OssLog entity)) {
            log.warn("[OssLogHandler] 收到非 OssLog 类型，跳过: {}", baseLog.getClass().getName());
            return;
        }
        String tenantId = entity.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            log.error("[OssLogHandler] 上传日志缺少租户标识，已拒绝持久化: file={}", entity.getOriginalName());
            return;
        }
        try {
            TenantExecutionScope.run(tenantId.trim(), () -> ossLogService.createEntity(entity));
            log.debug("[OssLogHandler] 上传日志保存成功: file={}, size={}, status={}",
                    entity.getOriginalName(), entity.getFileSize(), entity.getStatus());
        } catch (Exception e) {
            log.error("[OssLogHandler] 保存上传日志失败: file={}", entity.getOriginalName(), e);
        }
    }
}
