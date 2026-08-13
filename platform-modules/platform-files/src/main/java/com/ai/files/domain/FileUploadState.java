package com.ai.files.domain;

/**
 * 文件上传 saga 的持久化状态。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum FileUploadState {
    /** 数据库已预留节点、版本、配额与对象键。 */
    PREPARED,
    /** 对象存储已确认存在，等待发布元数据。 */
    OBJECT_STORED,
    /** 文件版本已发布为当前可用版本。 */
    COMPLETED,
    /** 对象可能存在，等待幂等删除并释放预留。 */
    CLEANUP_PENDING,
    /** 对象已确认不存在，预留已释放。 */
    CANCELLED
}
