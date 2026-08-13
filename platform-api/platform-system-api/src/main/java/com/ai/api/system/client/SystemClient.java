package com.ai.api.system.client;

import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import com.ai.api.system.dto.DictDTO;
import com.ai.api.system.dto.DeptIdentityDTO;
import com.ai.api.system.dto.ImportTemplateFieldDTO;
import com.ai.api.system.dto.ImportDefinitionDTO;
import com.ai.api.system.dto.PushPreferenceBatchRequest;
import com.ai.api.system.dto.UserPushPreferenceDTO;
import com.ai.api.system.dto.UserOrganizationDTO;
import com.ai.api.system.dto.UserIdentityBatchRequest;
import com.ai.api.system.dto.UserIdentityDTO;
import com.ai.api.system.dto.WeatherInfoDTO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * platform-system 服务声明式 HTTP 客户端（API 契约层）
 * <p>
 * 由 platform-system-api 模块统一定义，消费方只需引入该契约依赖即可使用。
 * 通过 Spring HTTP Interface（{@link HttpExchange}）声明式调用系统服务的内部接口，
 * 底层由负载均衡 WebClient 驱动，与 WebFlux 技术栈保持一致。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@HttpExchange(url = "/internal")
public interface SystemClient {

    /**
     * 在显式租户边界内批量查询用户推送偏好。
     *
     * <p>租户头只有同时携带平台内部可信令牌时才会被系统服务接受。</p>
     *
     * @param tenantId 租户标识
     * @param request  批量查询请求
     * @return 当前租户内请求用户的推送偏好；非本租户用户不返回
     */
    @PostExchange("/setting/push-preferences")
    Mono<List<UserPushPreferenceDTO>> getPushPreferencesForTenant(
            @RequestHeader(AuthConstants.HeaderConstants.TENANT_ID) String tenantId,
            @RequestBody PushPreferenceBatchRequest request
    );

    // ==================== 字典服务 ====================

    /**
     * 根据字典类型查询字典项列表
     *
     * @param type 字典类型
     * @return 字典项列表
     */
    @GetExchange("/dict/getByType")
    Mono<List<DictDTO>> getDictByType(@RequestParam("type") String type);

    // ==================== 导入模板字段映射服务 ====================

    /**
     * 根据模块获取导入字段映射列表
     *
     * @param module 模块标识
     * @return 导入字段映射列表
     */
    @GetExchange("/import-mapping/by-module")
    Mono<List<ImportTemplateFieldDTO>> getImportTemplateFieldByModule(@RequestParam("module") String module);

    /** 返回调用租户的全部启用导入定义。 */
    @GetExchange("/import-definition/enabled")
    Mono<List<ImportDefinitionDTO>> getEnabledImportDefinitions(@RequestParam("tenantId") String tenantId);

    // ==================== 天气服务 ====================

    /**
     * 根据部门关联区域查询今日最新天气。
     *
     * @param deptId 部门ID
     * @return 今日天气
     */
    @GetExchange("/weather/today-by-dept")
    Mono<WeatherInfoDTO> getTodayWeatherByDept(@RequestParam("deptId") String deptId);

    /** 查询当前租户内的部门身份信息。 */
    @GetExchange("/organization/dept")
    Mono<DeptIdentityDTO> getDept(@RequestParam("deptId") String deptId);

    /**
     * 在后台任务或事件处理场景中按显式租户查询部门。
     *
     * <p>租户头只有同时携带平台内部可信令牌时才会被系统服务接受。</p>
     */
    @GetExchange("/organization/dept")
    Mono<DeptIdentityDTO> getDeptForTenant(
            @RequestHeader(AuthConstants.HeaderConstants.TENANT_ID) String tenantId,
            @RequestParam("deptId") String deptId
    );

    /** 查询当前租户内的用户组织归属。 */
    @GetExchange("/organization/user")
    Mono<UserOrganizationDTO> getUserOrganization(@RequestParam("userId") String userId);

    /** 按显式租户查询用户组织归属，仅供受信任的内部服务调用。 */
    @GetExchange("/organization/user")
    Mono<UserOrganizationDTO> getUserOrganizationForTenant(
            @RequestHeader(AuthConstants.HeaderConstants.TENANT_ID) String tenantId,
            @RequestParam("userId") String userId
    );

    /**
     * 在当前租户内按用户名批量解析用户身份。
     *
     * <p>返回值只包含真实存在且已启用的用户，调用方必须对未返回的账号给出明确错误。</p>
     *
     * @param request 用户名批量请求
     * @return 当前租户内匹配的用户身份
     */
    @PostExchange("/organization/users/by-usernames")
    Mono<List<UserIdentityDTO>> getUsersByUsernames(@RequestBody UserIdentityBatchRequest request);

    /** 校验用户是否属于当前租户的指定部门。 */
    @GetExchange("/organization/user-in-dept")
    Mono<Boolean> isUserInDept(
            @RequestParam("userId") String userId,
            @RequestParam("deptId") String deptId
    );

    /** 按显式租户校验用户与部门关系，仅供受信任的内部服务调用。 */
    @GetExchange("/organization/user-in-dept")
    Mono<Boolean> isUserInDeptForTenant(
            @RequestHeader(AuthConstants.HeaderConstants.TENANT_ID) String tenantId,
            @RequestParam("userId") String userId,
            @RequestParam("deptId") String deptId
    );
}
