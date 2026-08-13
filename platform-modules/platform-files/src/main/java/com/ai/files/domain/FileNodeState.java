package com.ai.files.domain;

/**
 * 文件节点生命周期状态。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum FileNodeState {
    /** 新文件已预留元数据，对象内容尚未确认。 */
    UPLOADING,
    /** 正常可见。 */
    ACTIVE,
    /** 已进入回收站。 */
    TRASHED
}
