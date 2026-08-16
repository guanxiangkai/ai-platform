package com.ai.scheduler.service;

import com.ai.scheduler.config.SchedulerProperties;
import com.ai.scheduler.domain.ScheduleType;
import com.ai.scheduler.domain.SchedulerSyncState;
import com.ai.scheduler.domain.SchedulerTask;
import com.ai.scheduler.integration.PowerJobSchedulerGateway;
import com.ai.scheduler.repository.SchedulerTaskRepository;
import com.ai.scheduler.web.SchedulerTaskRequest;
import com.ai.scheduler.web.SchedulerViews;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.common.response.PageResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 通用定时任务的租户治理、持久化和 PowerJob 同步服务。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerTaskService {

    private final SchedulerTaskRepository repository;
    private final SchedulerProperties properties;
    private final PowerJobSchedulerGateway powerJobGateway;

    /** 返回当前租户允许使用的业务应用和处理器白名单。 */
    @Transactional(readOnly = true)
    public List<SchedulerViews.Application> applications() {
        String tenantId = currentTenantId();
        return properties.getApplications().entrySet().stream()
                .filter(entry -> tenantId.equals(entry.getValue().getTenantId()))
                .map(entry -> toApplicationView(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** 分页查询当前租户的任务定义。 */
    @Transactional(readOnly = true)
    public PageResponse<SchedulerViews.Task> list(int page, int size, String keyword, Boolean enabled) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(properties.getMaxPageSize(), Math.max(1, size));
        String tenantId = currentTenantId();
        Specification<SchedulerTask> specification = (root, query, builder) -> {
            var predicate = builder.and(
                    builder.equal(root.get("tenantId"), tenantId),
                    builder.isFalse(root.get("deleted")));
            if (enabled != null) {
                predicate = builder.and(predicate, builder.equal(root.get("enabled"), enabled));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("taskCode")), pattern),
                        builder.like(builder.lower(root.get("taskName")), pattern)));
            }
            return predicate;
        };
        Page<SchedulerTask> result = repository.findAll(specification,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createTime")));
        return PageResponse.of(result.getContent().stream().map(SchedulerViews.Task::from).toList(),
                result.getTotalElements(), safePage, safeSize);
    }

    /** 查询当前租户任务详情。 */
    @Transactional(readOnly = true)
    public SchedulerViews.Task detail(String id) {
        return SchedulerViews.Task.from(requireTask(id));
    }

    /** 保存任务期望配置，并立即尝试同步到 PowerJob。 */
    @Transactional(rollbackFor = Exception.class)
    public SchedulerViews.Task create(SchedulerTaskRequest request) {
        String tenantId = currentTenantId();
        String taskCode = normalized(request.taskCode());
        if (repository.existsByTenantIdAndTaskCodeAndDeletedFalse(tenantId, taskCode)) {
            throw new BizException("任务编码已存在");
        }
        SchedulerTask task = new SchedulerTask();
        task.setTenantId(tenantId);
        task.setDeleted(false);
        apply(task, request, false);
        repository.saveAndFlush(task);
        synchronize(task);
        return SchedulerViews.Task.from(task);
    }

    /** 更新任务期望配置，并立即尝试同步到 PowerJob。 */
    @Transactional(rollbackFor = Exception.class)
    public SchedulerViews.Task update(String id, SchedulerTaskRequest request) {
        SchedulerTask task = requireTask(id);
        if (!task.getApplicationCode().equals(normalized(request.applicationCode()))) {
            throw new BizException("任务所属应用不可变；如需变更，请删除任务后重新创建");
        }
        String taskCode = normalized(request.taskCode());
        if (repository.existsByTenantIdAndTaskCodeAndIdNotAndDeletedFalse(
                task.getTenantId(), taskCode, task.getId())) {
            throw new BizException("任务编码已存在");
        }
        apply(task, request, true);
        repository.saveAndFlush(task);
        synchronize(task);
        return SchedulerViews.Task.from(task);
    }

    /** 修改启用状态，并同步到 PowerJob。 */
    @Transactional(rollbackFor = Exception.class)
    public SchedulerViews.Task changeEnabled(String id, boolean enabled) {
        SchedulerTask task = requireTask(id);
        task.setEnabled(enabled);
        task.setSyncState(SchedulerSyncState.PENDING);
        repository.saveAndFlush(task);
        synchronize(task);
        return SchedulerViews.Task.from(task);
    }

    /** 手工重试本地定义到 PowerJob 的同步。 */
    @Transactional(rollbackFor = Exception.class)
    public SchedulerViews.Task synchronize(String id) {
        SchedulerTask task = requireTask(id);
        synchronize(task);
        return SchedulerViews.Task.from(task);
    }

    /** 手工触发一次已同步且启用的任务。 */
    @Transactional(readOnly = true)
    public Long run(String id, String instanceParameters) {
        SchedulerTask task = requireTask(id);
        if (!Boolean.TRUE.equals(task.getEnabled())) {
            throw new BizException("任务未启用，不能手工执行");
        }
        if (task.getPowerjobJobId() == null || task.getSyncState() != SchedulerSyncState.SYNCED) {
            throw new BizException("任务尚未成功同步到调度引擎");
        }
        SchedulerProperties.Application application = requireApplication(
                task.getApplicationCode(), task.getTenantId());
        String parameters = StringUtils.hasText(instanceParameters)
                ? instanceParameters.trim() : task.getJobParameters();
        return powerJobGateway.run(task.getApplicationCode(), application,
                task.getPowerjobJobId(), parameters);
    }

    /** 查询当前任务在 PowerJob 中的执行实例。 */
    @Transactional(readOnly = true)
    public PageResponse<SchedulerViews.Instance> instances(String id, int page, int size) {
        SchedulerTask task = requireTask(id);
        if (task.getPowerjobJobId() == null) {
            return PageResponse.of(List.of(), 0, Math.max(1, page), Math.max(1, size));
        }
        int safePage = Math.max(1, page);
        int safeSize = Math.min(properties.getMaxPageSize(), Math.max(1, size));
        SchedulerProperties.Application application = requireApplication(
                task.getApplicationCode(), task.getTenantId());
        PageResult<InstanceInfoDTO> result = powerJobGateway.instances(task.getApplicationCode(),
                application, task.getPowerjobJobId(), safePage - 1, safeSize);
        List<SchedulerViews.Instance> items = result.getData() == null ? List.of()
                : result.getData().stream().map(this::toInstanceView).toList();
        return PageResponse.of(items, result.getTotalItems(), safePage, safeSize);
    }

    /** 先删除远端任务，再软删除本地定义，避免产生仍会触发的孤儿任务。 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        SchedulerTask task = requireTask(id);
        if (task.getPowerjobJobId() != null) {
            SchedulerProperties.Application application = requireApplication(
                    task.getApplicationCode(), task.getTenantId());
            powerJobGateway.delete(task.getApplicationCode(), application, task.getPowerjobJobId());
        }
        task.setEnabled(false);
        task.setDeleted(true);
        task.setSyncState(SchedulerSyncState.SYNCED);
        task.setLastSyncTime(LocalDateTime.now());
        task.setLastSyncMessage("任务已从调度引擎删除");
        repository.save(task);
    }

    private void apply(SchedulerTask task, SchedulerTaskRequest request, boolean updating) {
        String tenantId = currentTenantId();
        String applicationCode = normalized(request.applicationCode());
        SchedulerProperties.Application application = requireApplication(applicationCode, tenantId);
        validateHandler(application, request.processorInfo());
        validateExpression(request.timeExpressionType(), request.timeExpression());

        if (updating && task.getPowerjobJobId() != null
                && !Objects.equals(task.getApplicationName(), application.getAppName())) {
            throw new BizException("PowerJob 应用映射已变化，请恢复原映射并删除任务后重新创建");
        }
        task.setTaskCode(normalized(request.taskCode()));
        task.setTaskName(normalized(request.taskName()));
        task.setApplicationCode(applicationCode);
        task.setApplicationName(normalized(application.getAppName()));
        task.setProcessorInfo(normalized(request.processorInfo()));
        task.setTimeExpressionType(request.timeExpressionType());
        task.setTimeExpression(normalized(request.timeExpression()));
        task.setJobParameters(trimToNull(request.jobParameters()));
        task.setMaxInstanceNum(bounded(request.maxInstanceNum(), 1, 0,
                properties.getMaxInstanceCount(), "最大实例数"));
        task.setConcurrency(bounded(request.concurrency(), 1, 1,
                properties.getMaxConcurrency(), "并发线程数"));
        task.setInstanceTimeLimit(bounded(request.instanceTimeLimit(), 0L, 0L,
                properties.getMaxInstanceTimeLimitMs(), "实例最长执行时间"));
        task.setInstanceRetryNum(bounded(request.instanceRetryNum(), 0, 0,
                properties.getMaxRetryCount(), "实例重试次数"));
        task.setTaskRetryNum(bounded(request.taskRetryNum(), 0, 0,
                properties.getMaxRetryCount(), "任务重试次数"));
        task.setEnabled(request.enabled() == null || request.enabled());
        task.setRemark(trimToNull(request.remark()));
        task.setSyncState(SchedulerSyncState.PENDING);
        task.setLastSyncMessage("等待同步到调度引擎");
    }

    private void synchronize(SchedulerTask task) {
        SchedulerProperties.Application application = requireApplication(
                task.getApplicationCode(), task.getTenantId());
        try {
            Long jobId = powerJobGateway.save(task.getApplicationCode(), application, task);
            task.setPowerjobJobId(jobId);
            task.setApplicationName(application.getAppName().trim());
            task.setSyncState(SchedulerSyncState.SYNCED);
            task.setLastSyncTime(LocalDateTime.now());
            task.setLastSyncMessage("已同步到调度引擎");
        } catch (RuntimeException exception) {
            task.setSyncState(SchedulerSyncState.FAILED);
            task.setLastSyncTime(LocalDateTime.now());
            task.setLastSyncMessage("调度引擎同步失败");
            log.warn("调度任务同步失败: taskId={}, applicationCode={}, exception={}",
                    task.getId(), task.getApplicationCode(), exception.getClass().getSimpleName());
        }
        repository.saveAndFlush(task);
    }

    private SchedulerTask requireTask(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BizException("任务标识不能为空");
        }
        return repository.findByIdAndTenantIdAndDeletedFalse(id.trim(), currentTenantId())
                .orElseThrow(() -> new BizException("定时任务不存在或无权访问"));
    }

    private SchedulerProperties.Application requireApplication(String code, String tenantId) {
        SchedulerProperties.Application application = properties.getApplications().get(code);
        if (application == null || !tenantId.equals(application.getTenantId())) {
            throw new BizException("当前租户不可使用该调度应用");
        }
        if (!StringUtils.hasText(application.getAppName())) {
            throw new BizException("调度应用名称未配置");
        }
        return application;
    }

    private void validateHandler(SchedulerProperties.Application application, String processorInfo) {
        String normalized = normalized(processorInfo);
        boolean allowed = application.getHandlers() != null && application.getHandlers().stream()
                .anyMatch(handler -> normalized.equals(handler.getProcessorInfo()));
        if (!allowed) {
            throw new BizException("处理器不在当前应用的允许目录中");
        }
    }

    private void validateExpression(ScheduleType type, String expression) {
        if (type == null || !StringUtils.hasText(expression)) {
            throw new BizException("调度类型和时间表达式不能为空");
        }
        String normalized = expression.trim();
        if (type == ScheduleType.CRON) {
            int fields = normalized.split("\\s+").length;
            if (fields < 6 || fields > 7) {
                throw new BizException("Cron 表达式必须包含 6 或 7 个字段");
            }
            return;
        }
        try {
            long interval = Long.parseLong(normalized);
            if (interval < properties.getMinFixedIntervalMs()) {
                throw new BizException("固定频率或固定延迟不得小于 "
                        + properties.getMinFixedIntervalMs() + " 毫秒");
            }
        } catch (NumberFormatException exception) {
            throw new BizException("固定频率或固定延迟必须填写毫秒整数");
        }
    }

    private SchedulerViews.Application toApplicationView(String code, SchedulerProperties.Application application) {
        List<SchedulerViews.Handler> handlers = application.getHandlers() == null ? List.of()
                : application.getHandlers().stream()
                .map(handler -> new SchedulerViews.Handler(handler.getProcessorInfo(),
                        handler.getDisplayName(), handler.getDescription()))
                .toList();
        return new SchedulerViews.Application(code, application.getDisplayName(),
                application.getAppName(), handlers);
    }

    private SchedulerViews.Instance toInstanceView(InstanceInfoDTO instance) {
        InstanceStatus status;
        try {
            status = InstanceStatus.of(instance.getStatus());
        } catch (IllegalArgumentException ignored) {
            status = null;
        }
        return new SchedulerViews.Instance(
                instance.getInstanceId(), instance.getJobId(), instance.getStatus(),
                status == null ? "UNKNOWN" : status.name(),
                status == null ? "未知" : status.getDes(),
                instance.getJobParams(), instance.getInstanceParams(), instance.getResult(),
                instant(instance.getExpectedTriggerTime()), instant(instance.getActualTriggerTime()),
                instant(instance.getFinishedTime()), instance.getRunningTimes(),
                instant(instance.getGmtCreate()), instant(instance.getGmtModified()));
    }

    private String currentTenantId() {
        String tenantId = SecurityUtils.getTenantId();
        if (!StringUtils.hasText(tenantId) || "0".equals(tenantId)) {
            throw new BizException("未获取到产品租户上下文，请从已配置的产品入口访问调度管理");
        }
        return tenantId.trim();
    }

    private String normalized(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("必填字段不能为空");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int bounded(Integer value, int fallback, int min, int max, String label) {
        int resolved = value == null ? fallback : value;
        if (resolved < min || resolved > max) {
            throw new BizException(label + "必须在 " + min + " 到 " + max + " 之间");
        }
        return resolved;
    }

    private long bounded(Long value, long fallback, long min, long max, String label) {
        long resolved = value == null ? fallback : value;
        if (resolved < min || resolved > max) {
            throw new BizException(label + "必须在 " + min + " 到 " + max + " 之间");
        }
        return resolved;
    }

    private Instant instant(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private Instant instant(Date value) {
        return value == null ? null : value.toInstant();
    }
}
