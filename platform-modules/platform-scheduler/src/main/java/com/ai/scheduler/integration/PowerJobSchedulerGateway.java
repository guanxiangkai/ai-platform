package com.ai.scheduler.integration;

import com.ai.scheduler.config.SchedulerProperties;
import com.ai.scheduler.domain.SchedulerTask;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.request.query.InstancePageQuery;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.common.response.PageResult;
import tech.powerjob.common.response.ResultDTO;

/** 将平台任务模型适配为 PowerJob 5.1.2 OpenAPI 调用。 */
@Component
@RequiredArgsConstructor
public class PowerJobSchedulerGateway {

    private final PowerJobClientRegistry clientRegistry;

    /** 创建或更新 PowerJob 任务并返回任务标识。 */
    public Long save(String applicationCode, SchedulerProperties.Application application, SchedulerTask task) {
        SaveJobInfoRequest request = new SaveJobInfoRequest();
        request.setId(task.getPowerjobJobId());
        request.setJobName(task.getTaskName());
        request.setJobDescription(task.getRemark());
        request.setJobParams(task.getJobParameters());
        request.setTimeExpressionType(TimeExpressionType.valueOf(task.getTimeExpressionType().name()));
        request.setTimeExpression(task.getTimeExpression());
        request.setExecuteType(ExecuteType.STANDALONE);
        request.setProcessorType(ProcessorType.BUILT_IN);
        request.setProcessorInfo(task.getProcessorInfo());
        request.setMaxInstanceNum(task.getMaxInstanceNum());
        request.setConcurrency(task.getConcurrency());
        request.setInstanceTimeLimit(task.getInstanceTimeLimit());
        request.setInstanceRetryNum(task.getInstanceRetryNum());
        request.setTaskRetryNum(task.getTaskRetryNum());
        request.setEnable(Boolean.TRUE.equals(task.getEnabled()));
        request.setTag("platform-scheduler:" + task.getTaskCode());
        return requireData(client(applicationCode, application).saveJob(request), "同步 PowerJob 任务失败");
    }

    /** 删除 PowerJob 中的任务定义。 */
    public void delete(String applicationCode, SchedulerProperties.Application application, Long jobId) {
        requireSuccess(client(applicationCode, application).deleteJob(jobId), "删除 PowerJob 任务失败");
    }

    /** 手工触发一次任务并返回实例标识。 */
    public Long run(String applicationCode, SchedulerProperties.Application application,
                    Long jobId, String instanceParameters) {
        return requireData(client(applicationCode, application)
                .runJob(jobId, instanceParameters, 0L), "触发 PowerJob 任务失败");
    }

    /** 查询指定任务的 PowerJob 执行实例。 */
    public PageResult<InstanceInfoDTO> instances(String applicationCode,
                                                 SchedulerProperties.Application application,
                                                 Long jobId, int pageIndex, int pageSize) {
        InstancePageQuery query = new InstancePageQuery();
        query.setIndex(pageIndex);
        query.setPageSize(pageSize);
        query.setJobIdEq(jobId);
        query.setSortBy("gmtCreate");
        query.setAsc(false);
        return requireData(client(applicationCode, application)
                .queryInstanceInfo(query), "查询 PowerJob 执行实例失败");
    }

    private PowerJobClient client(String applicationCode, SchedulerProperties.Application application) {
        return clientRegistry.client(applicationCode, application);
    }

    private <T> T requireData(ResultDTO<T> result, String fallback) {
        requireSuccess(result, fallback);
        if (result.getData() == null) {
            throw new BizException(fallback + "：返回数据为空");
        }
        return result.getData();
    }

    private void requireSuccess(ResultDTO<?> result, String fallback) {
        if (result == null || !result.isSuccess()) {
            String message = result == null ? null : result.getMessage();
            throw new BizException(message == null || message.isBlank() ? fallback : fallback + "：" + message);
        }
    }
}
