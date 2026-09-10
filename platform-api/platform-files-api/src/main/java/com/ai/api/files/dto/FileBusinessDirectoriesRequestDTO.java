package com.ai.api.files.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 已存在业务根目录下需要确保存在的相对目录列表。 */
public record FileBusinessDirectoriesRequestDTO(
        @NotBlank @Size(max = 64) String rootBusinessType,
        @NotBlank @Size(max = 128) String rootBusinessId,
        @NotEmpty @Size(max = 8000) List<@NotBlank @Size(max = 1024) String> relativePaths
) { }
