package com.ai.files.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.Instant;

/** 防止多人同时覆盖同一文件的短时编辑锁。 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "file_edit_lock", comment = "文件编辑锁表")
public class FileEditLock extends DataTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 被锁定的文件节点标识。 */
    @Column(name = "node_id", nullable = false, length = 64, comment = "文件节点标识")
    private String nodeId;

    /** 锁持有人。 */
    @Column(name = "owner_user_id", nullable = false, length = 64, comment = "锁持有人用户标识")
    private String ownerUserId;

    /** 仅保存返回令牌的 SHA-256，不保存明文令牌。 */
    @Column(name = "token_hash", nullable = false, length = 64, comment = "编辑锁令牌哈希值")
    private String tokenHash;

    /** 锁自动失效时间。 */
    @Column(name = "expires_at", nullable = false, comment = "编辑锁过期时间")
    private Instant expiresAt;
}
