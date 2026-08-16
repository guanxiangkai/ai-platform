package com.ai.system.service.impl;

import cn.hutool.extra.pinyin.PinyinUtil;
import io.github.guanxiangkai.jpa.plus.interceptor.tenant.spi.TenantIdProvider;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import com.ai.system.domain.dto.RegisterCreateDTO;
import com.ai.system.domain.dto.RegisterDTO;
import com.ai.system.domain.dto.RegisterPageDTO;
import com.ai.system.domain.entity.Dept;
import com.ai.system.domain.entity.Register;
import com.ai.system.domain.entity.Role;
import com.ai.system.domain.entity.User;
import com.ai.system.domain.entity.UserRole;
import com.ai.system.domain.entity.RegistrationOutbox;
import com.ai.system.domain.OutboxDeliveryState;
import com.ai.system.domain.RegisterState;
import com.ai.system.domain.RegistrationOutboxEventType;
import com.ai.system.domain.vo.RegisterAuditResultVO;
import com.ai.system.domain.vo.RegisterPageVO;
import com.ai.system.domain.vo.RegisterResultVO;
import com.ai.system.domain.vo.RegisterVO;
import com.ai.system.repository.DeptRepository;
import com.ai.system.repository.RegisterRepository;
import com.ai.system.repository.RoleRepository;
import com.ai.system.repository.UserRepository;
import com.ai.system.repository.UserRoleRepository;
import com.ai.system.repository.RegistrationOutboxRepository;
import com.ai.system.constants.SystemConstants;
import com.ai.system.integration.TenantWorkforceClient;
import com.ai.system.integration.WorkforceMatchResult;
import com.ai.system.integration.WorkforcePosition;
import com.ai.system.security.AuthUserCacheService;
import com.ai.system.service.IRegisterService;
import com.ai.system.service.RegistrationSubmissionTransactionService;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 注册记录服务实现
 *
 * <p>注册流程：
 * <ol>
 *   <li>用户只需填写真实姓名、密码、部门，其余选填</li>
 *   <li>注册时立即调用业务服务匹配员工档案（姓名→手机→邮箱）</li>
 *   <li>匹配失败（未找到 / 已有账户）→ 注册拒绝</li>
 *   <li>根据真实姓名拼音首字母自动生成用户名，重复时追加随机后缀</li>
 *   <li>保存注册记录（PENDING），返回生成的用户名让用户记下来</li>
 * </ol>
 * 审核流程：
 * <ol>
 *   <li>审核通过：仅创建 User 账户，回填 userId</li>
 *   <li>通知业务服务将 User 绑定到员工档案（hasUserAccount=true, userId=...）</li>
 *   <li>同时为 User 绑定注册时选择的部门</li>
 *   <li>审核拒绝：仅更新状态</li>
 * </ol>
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterServiceImpl
        extends BaseServiceImpl<RegisterPageDTO, RegisterPageVO, RegisterVO, RegisterCreateDTO, RegisterDTO, Register>
        implements IRegisterService {



    private final RegisterRepository repository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final DeptRepository deptRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantWorkforceClient workforceClient;
    private final AuthUserCacheService authUserCacheService;
    private final TenantIdProvider tenantIdProvider;
    private final RegistrationOutboxRepository registrationOutboxRepository;
    private final RegistrationSubmissionTransactionService submissionTransactionService;

    @Override
    protected BaseRepository<RegisterPageVO, RegisterVO, Register> getRepository() {
        return this.repository;
    }

    // ==================== 注册 ====================

    @Override
    public RegisterResultVO register(RegisterCreateDTO dto) {

        Dept dept = deptRepository.findByIdAndDeletedFalse(dto.deptId())
                .orElseThrow(() -> new BizException("所选部门不存在或已停用"));

        // 2. 校验邮箱唯一性
        if (StringUtils.hasText(dto.email())) {
            if (repository.existsByEmailAndDeletedFalse(dto.email())) {
                throw new BizException("该邮箱已存在注册申请");
            }
            if (userRepository.existsByEmailAndDeletedFalse(dto.email())) {
                throw new BizException("该邮箱已被使用");
            }
        }

        // 3. 校验手机号唯一性
        if (StringUtils.hasText(dto.phone())) {
            if (repository.existsByPhoneAndDeletedFalse(dto.phone())) {
                throw new BizException("该手机号已存在注册申请");
            }
            if (userRepository.existsByPhoneAndDeletedFalse(dto.phone())) {
                throw new BizException("该手机号已被使用");
            }
        }

        // 4. 调用业务服务匹配员工档案（注册时立即匹配，匹配失败则拒绝注册）
        WorkforceMatchResult matchResult;
        try {
            matchResult = workforceClient
                    .matchForRegister(dto.realName(), dto.deptId(), dto.phone(), dto.email())
                    .block();
        } catch (Exception e) {
            log.error("调用业务服务匹配员工档案失败: exception={}", e.getClass().getSimpleName());
            throw new BizException("员工信息查询失败，请稍后重试");
        }

        if (matchResult == null || matchResult.personnelId() == null) {
            if (matchResult != null && matchResult.alreadyHasAccount()) {
                throw new BizException("匹配到的员工档案已绑定系统账户，如有问题请联系管理员");
            }
            throw new BizException("未找到匹配的员工档案，请确认姓名/手机/邮箱是否与档案一致，或联系管理员");
        }
        if (matchResult.alreadyHasAccount()) {
            throw new BizException("该员工档案已绑定系统账户，如有疑问请联系管理员");
        }

        // 5. 自动生成唯一用户名
        String username = generateUniqueUsername(dept, dto.realName());

        // 6. 构建并保存注册记录
        Register record = new Register();
        record.setRealName(dto.realName());
        record.setPassword(passwordEncoder.encode(dto.password()));
        record.setNickname(dto.nickname());
        record.setEmail(dto.email());
        record.setPhone(dto.phone());
        record.setGender(dto.gender());
        record.setDeptId(dto.deptId());
        record.setPersonnelId(matchResult.personnelId());
        record.setUsername(username);
        Register saved = submissionTransactionService.submit(record);
        log.info("用户注册申请已提交: recordId={}", saved.getId());

        return new RegisterResultVO(saved.getId(), username, matchResult.personnelName());
    }

    @Override
    public Optional<Register> findByUsernameAndDeletedFalse(String username) {
        return repository.findByUsernameAndDeletedFalse(username);
    }

    // ==================== 审核 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterAuditResultVO audit(String id, Boolean approved, String auditRemark) {
        Register record = repository.findLockedById(id)
                .orElseThrow(() -> BizException.notFound("注册记录不存在"));

        if (record.getRegistrationState() != RegisterState.PENDING) {
            throw new BizException("该注册申请已审核，无法重复操作");
        }

        String auditorId = SecurityUtils.getUserId();
        record.setAuditBy(auditorId);
        record.setAuditTime(LocalDateTime.now());
        record.setAuditRemark(auditRemark);

        if (Boolean.TRUE.equals(approved)) {
            record.setRegistrationState(RegisterState.PROVISIONING);

            // 再次检查用户名是否在审核期间被占用
            if (userRepository.existsByUsernameAndDeletedFalse(record.getUsername())) {
                // 重新生成一个新用户名
                Dept registerDept = deptRepository.findByIdAndDeletedFalse(record.getDeptId())
                        .orElseThrow(() -> new BizException("注册部门不存在或已停用"));
                String newUsername = generateUniqueUsername(registerDept, record.getRealName());
                log.warn("原用户名已被占用，已重新生成: recordId={}", record.getId());
                record.setUsername(newUsername);
            }

            // 创建用户账户（仅此操作，不创建员工档案）
            User user = new User();
            user.setUsername(record.getUsername());
            user.setPassword(record.getPassword());   // 注册时已加密
            user.setNickname(record.getNickname());
            user.setRealName(record.getRealName());
            user.setEmail(record.getEmail());
            user.setPhone(record.getPhone());
            user.setGender(record.getGender() != null ? Integer.parseInt(record.getGender()) : null);
            user.setDeptId(record.getDeptId());
            user.setEnabled(false);
            user.setSortOrder(0);

            User savedUser = userRepository.save(user);
            record.setUserId(savedUser.getId());
            log.info("注册审核通过，用户账户创建成功: recordId={}", id);

            RegistrationOutbox outbox = new RegistrationOutbox();
            outbox.setAggregateId(record.getId());
            outbox.setEventType(RegistrationOutboxEventType.REGISTER_PROVISION);
            outbox.setDeliveryState(OutboxDeliveryState.PENDING);
            outbox.setAvailableAt(LocalDateTime.now());
            registrationOutboxRepository.save(outbox);
        } else {
            record.setRegistrationState(RegisterState.REJECTED);
            log.info("注册申请已拒绝: recordId={}", id);
        }

        repository.save(record);
        return new RegisterAuditResultVO(record.getId(), record.getRegistrationState());
    }


    @Override
    public String create(RegisterCreateDTO dto) {
        return register(dto).id();
    }

    // ==================== 用户名生成 ====================

    /**
     * 根据真实姓名生成拼音首字母用户名，重复时追加随机字母数字后缀
     */
    private String generateUniqueUsername(Dept dept, String realName) {
        String deptPrefix = generateDeptUsernamePrefix(dept);
        String namePart = generateUsernameBase(realName);
        String base = deptPrefix + "_" + namePart;
        if (!usernameExists(base)) {
            return base;
        }
        for (int suffix = 1; suffix <= 9999; suffix++) {
            String candidate = base + suffix;
            if (!usernameExists(candidate)) {
                return candidate;
            }
        }
        return base + System.currentTimeMillis();
    }

    private String generateDeptUsernamePrefix(Dept dept) {
        if (dept == null) {
            return "dept";
        }
        if (StringUtils.hasText(dept.getDeptCode())) {
            return dept.getDeptCode().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        }
        String prefix = generateUsernameBase(dept.getDeptName());
        if (StringUtils.hasText(prefix) && !"user".equals(prefix)) {
            return prefix;
        }
        if (StringUtils.hasText(dept.getId())) {
            String normalizedId = dept.getId().replaceAll("[^a-zA-Z0-9]", "");
            if (StringUtils.hasText(normalizedId)) {
                return "d" + normalizedId.substring(0, Math.min(8, normalizedId.length())).toLowerCase();
            }
        }
        return "dept";
    }

    /**
     * 将真实姓名转为拼音首字母（如 "张三" → "zs"）
     */
    private String generateUsernameBase(String realName) {
        if (!StringUtils.hasText(realName)) {
            return "user";
        }
        try {
            String initials = PinyinUtil.getFirstLetter(realName.trim(), "")
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]", "");
            return StringUtils.hasText(initials) ? initials : "user";
        } catch (Exception e) {
            // Pinyin 引擎不可用时，取 ASCII 字母首字母作为兜底
            StringBuilder sb = new StringBuilder();
            for (char c : realName.toCharArray()) {
                if (c >= 'A' && c <= 'Z') sb.append(Character.toLowerCase(c));
                else if (c >= 'a' && c <= 'z') sb.append(c);
            }
            return sb.length() > 0 ? sb.toString() : "user";
        }
    }

    private boolean usernameExists(String username) {
        return repository.existsByUsernameAndDeletedFalse(username)
                || userRepository.existsByUsernameAndDeletedFalse(username);
    }

    private UserRole newUserRole(String userId, String roleId) {
        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .build();
        return RelationEntityDefaults.ensure(userRole, tenantIdProvider);
    }

    // ==================== 查询构建 ====================

    @Override
    protected Specification<Register> buildQuerySpec(RegisterPageDTO pageDTO) {
        if (pageDTO == null) return (root, query, cb) -> cb.conjunction();
        return SpecUtils.<Register>builder()
                .andIf(StringUtils.hasText(pageDTO.getName()), () -> {
                    String pattern = "%" + pageDTO.getName() + "%";
                    return (root, query, cb) -> cb.or(
                            cb.like(root.get("username"), pattern),
                            cb.like(root.get("nickname"), pattern),
                            cb.like(root.get("realName"), pattern)
                    );
                })
                .likeIfPresent(Register::getEmail, pageDTO.getEmail())
                .likeIfPresent(Register::getPhone, pageDTO.getPhone())
                .eqIfPresent(Register::getRegistrationState, pageDTO.getRegistrationState())
                .eqIfPresent(Register::getDeptId, pageDTO.getDeptId())
                .build();
    }

    @Override
    protected Sort buildSort(RegisterPageDTO pageDTO) {
        Sort.Order primary = null;
        if (pageDTO != null && StringUtils.hasText(pageDTO.getSortBy())) {
            primary = pageDTO.isAsc()
                    ? Sort.Order.asc(pageDTO.getSortBy())
                    : Sort.Order.desc(pageDTO.getSortBy());
        }
        Sort.Order fallback = Sort.Order.desc("createTime");
        return primary != null ? Sort.by(primary, fallback) : Sort.by(fallback);
    }
}
