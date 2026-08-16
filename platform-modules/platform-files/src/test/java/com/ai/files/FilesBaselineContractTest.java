package com.ai.files;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** 文件中心数据库基线契约测试。 */
class FilesBaselineContractTest {

    @Test
    void baselineShouldDefineNormalizedFilesModelAndTenantIndexes() throws IOException {
        String baseline = Files.readString(Path.of(System.getProperty("platformFilesBaseline")));

        assertThat(baseline)
                .contains("CREATE TABLE public.file_space")
                .contains("CREATE TABLE public.file_node")
                .contains("CREATE TABLE public.file_version")
                .contains("CREATE TABLE public.file_upload_record")
                .contains("CREATE TABLE public.file_grant")
                .contains("CREATE TABLE public.file_edit_lock")
                .contains("CREATE TABLE public.file_operation_log")
                .contains("tenant_id")
                .contains("uk_file_node_name_active")
                .contains("uk_file_version_object_key")
                .contains("uk_file_upload_node_open")
                .contains("idx_file_upload_reconcile")
                .contains("reserved_bytes bigint NOT NULL DEFAULT 0")
                .contains("active_upload_id varchar(64)")
                .contains("version_state varchar(16) NOT NULL DEFAULT 'PENDING'");
    }
}
