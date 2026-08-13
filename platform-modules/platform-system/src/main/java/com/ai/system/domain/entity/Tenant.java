package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataEntity;
import io.github.guanxiangkai.web.plus.core.entity.SortInfo;
import io.github.guanxiangkai.web.plus.core.entity.Sortable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 租户实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_tenant", comment = "租户表")
public class Tenant extends DataEntity implements Sortable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder.Default
    @Embedded
    private SortInfo sortInfo = new SortInfo();

    @Column(name = "tenant_name", nullable = false, length = 128, comment = "租户名称")
    private String tenantName;

    @Column(name = "tenant_code", length = 64, comment = "租户编码")
    private String tenantCode;

    @Column(name = "contact_name", length = 64, comment = "联系人")
    private String contactName;

    @Column(name = "contact_phone", length = 20, comment = "联系电话")
    private String contactPhone;

    @Column(name = "contact_email", length = 128, comment = "联系邮箱")
    private String contactEmail;

    @Column(name = "address", length = 255, comment = "租户地址")
    private String address;

    @Column(name = "expire_time", comment = "过期时间")
    private LocalDateTime expireTime;

    @Column(name = "user_limit", comment = "用户数量限制")
    private Integer userLimit;

    @Column(name = "logo", length = 500, comment = "租户Logo URL")
    private String logo;

    @Column(name = "domain", length = 128, comment = "租户域名")
    private String domain;

    @Column(name = "description", length = 500, comment = "租户描述")
    private String description;
}
