package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.SortableTenantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 用户岗位关系实体
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, exclude = {"user"})
@Entity
@Table(name = "sys_user_post", comment = "用户岗位关系表")
public class UserPost extends SortableTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 64, comment = "用户ID")
    private String userId;

    /**
     * 岗位ID
     */
    @Column(name = "post_id", nullable = false, length = 64, comment = "岗位ID")
    private String postId;

    /**
     * 岗位类型：借调岗、临时岗等
     */
    @Column(name = "post_type", length = 32, comment = "岗位类型：借调岗、临时岗等")
    private String postType;

    /**
     * 是否主岗
     */
    @Column(name = "main_post", comment = "是否主岗：true/false")
    private Boolean mainPost;

    /**
     * 任职开始日期
     */
    @Column(name = "start_date", comment = "任职开始日期")
    private LocalDateTime startDate;

    /**
     * 任职结束日期（为空表示当前有效，借调结束后变为失效）
     */
    @Column(name = "end_date", comment = "任职结束日期（为空表示当前有效）")
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false, comment = "用户ID")
    @ToString.Exclude
    private User user;
}
