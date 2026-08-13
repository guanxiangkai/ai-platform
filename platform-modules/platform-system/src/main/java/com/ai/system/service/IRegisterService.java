package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import com.ai.system.domain.dto.RegisterCreateDTO;
import com.ai.system.domain.dto.RegisterDTO;
import com.ai.system.domain.dto.RegisterPageDTO;
import com.ai.system.domain.entity.Register;
import com.ai.system.domain.vo.RegisterPageVO;
import com.ai.system.domain.vo.RegisterResultVO;
import com.ai.system.domain.vo.RegisterVO;
import com.ai.system.domain.vo.RegisterAuditResultVO;

import java.util.Optional;

/**
 * 注册记录服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IRegisterService extends IBaseService<RegisterPageDTO, RegisterPageVO, RegisterVO, RegisterCreateDTO, RegisterDTO, Register> {

    /**
     * 用户自助注册申请
     * <p>
     * 流程：
     * <ol>
     *   <li>校验真实姓名/邮箱/手机号唯一性</li>
     *   <li>调用业务服务匹配员工档案（按姓名→手机→邮箱顺序）</li>
     *   <li>未找到员工或员工已有账户时直接拒绝注册</li>
     *   <li>根据真实姓名拼音首字母自动生成用户名（重复时追加随机后缀）</li>
     *   <li>保存注册记录（状态PENDING），返回生成的用户名供用户记录</li>
     * </ol>
     * </p>
     *
     * @param dto 注册信息（真实姓名、密码、部门ID为必填）
     * @return 注册结果（包含自动生成的用户名，请提示用户牢记）
     */
    RegisterResultVO register(RegisterCreateDTO dto);

    /**
     * 根据用户名查询注册记录（用于登录时判断审核状态）
     */
    Optional<Register> findByUsernameAndDeletedFalse(String username);

    /**
     * 管理员审核注册记录
     * <p>
     * 审核通过时：创建用户账户，回填userId，并通知业务服务绑定员工账户。<br>
     * 审核拒绝时：仅更新状态。
     * </p>
     *
     * @param id          注册记录ID
     * @param approved    是否通过
     * @param auditRemark 审核备注
     * @return 是否成功
     */
    RegisterAuditResultVO audit(String id, Boolean approved, String auditRemark);
}
