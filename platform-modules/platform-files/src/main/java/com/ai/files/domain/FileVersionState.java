package com.ai.files.domain;

/**
 * 文件版本内容的可用状态。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum FileVersionState {
    /** 版本号和对象键已预留，内容尚不可读。 */
    PENDING,
    /** 对象内容与元数据均已确认。 */
    AVAILABLE,
    /** 上传未完成且对象已确认不存在。 */
    CANCELLED
}
