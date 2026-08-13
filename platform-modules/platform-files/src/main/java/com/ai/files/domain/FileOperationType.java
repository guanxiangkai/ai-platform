package com.ai.files.domain;

/** 文件审计操作类型。 */
public enum FileOperationType {
    CREATE_FOLDER,
    DOWNLOAD,
    CREATE_ACCESS_URL,
    RESTORE_VERSION,
    RENAME,
    MOVE,
    TRASH,
    RESTORE,
    UPSERT_GRANT,
    DELETE_GRANT,
    ACQUIRE_LOCK,
    RELEASE_LOCK,
    CREATE_SPACE,
    UPLOAD,
    CREATE_VERSION
}
