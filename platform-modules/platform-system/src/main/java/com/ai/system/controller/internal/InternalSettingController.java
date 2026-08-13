package com.ai.system.controller.internal;

import com.ai.api.system.dto.PushPreferenceBatchRequest;
import com.ai.api.system.dto.UserPushPreferenceDTO;
import com.ai.system.service.ISettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设置内部接口（仅供微服务间 RPC 调用）
 * <p>
 * 路径前缀 {@code /internal/setting}，由网关白名单控制访问，不对外暴露。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RestController
@RequestMapping("/internal/setting")
@RequiredArgsConstructor
public class InternalSettingController {

    private final ISettingService settingService;

    /**
     * 在当前可信租户上下文内批量查询用户推送偏好。
     *
     * @param request 批量查询请求
     * @return 按请求顺序返回的用户推送偏好
     */
    @PostMapping("/push-preferences")
    public List<UserPushPreferenceDTO> getPushPreferences(@RequestBody PushPreferenceBatchRequest request) {
        return settingService.getPushPreferences(request);
    }
}
