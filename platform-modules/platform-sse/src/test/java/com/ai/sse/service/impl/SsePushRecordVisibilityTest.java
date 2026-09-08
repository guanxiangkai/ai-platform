package com.ai.sse.service.impl;

import com.ai.api.security.PlatformSuperAdmin;
import com.ai.sse.config.SseProperties;
import com.ai.sse.domain.dto.SsePushRecordDTO;
import com.ai.sse.domain.entity.SsePushRecord;
import com.ai.sse.domain.vo.SsePushRecordVO;
import com.ai.sse.repository.SsePushRecordRepository;
import io.github.guanxiangkai.web.plus.core.converter.EntityConverter;
import io.github.guanxiangkai.web.plus.core.exception.CoreBizException;
import io.github.guanxiangkai.web.plus.security.context.UserContext;
import io.github.guanxiangkai.web.plus.security.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SsePushRecordVisibilityTest {

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void detailReturnsTheCheckedRecordWithoutReadingItAgain() {
        ordinaryUser();
        SsePushRecordRepository repository = mock(SsePushRecordRepository.class);
        SsePushRecordServiceImpl service = new SsePushRecordServiceImpl(repository, mock(SseProperties.class));
        SsePushRecord visible = record("user-1");
        SsePushRecord restricted = record(PlatformSuperAdmin.USER_ID);
        SsePushRecordVO expected = new SsePushRecordVO();
        when(repository.entityClass()).thenReturn(SsePushRecord.class);
        when(repository.findById("record-1")).thenReturn(Optional.of(visible), Optional.of(restricted));
        when(repository.detailVoClass()).thenReturn(SsePushRecordVO.class);

        try (var converter = mockStatic(EntityConverter.class)) {
            converter.when(() -> EntityConverter.toVo(visible, SsePushRecordVO.class)).thenReturn(expected);

            assertThat(service.detail("record-1")).isSameAs(expected);
            verify(repository, times(1)).findById("record-1");
        }
    }

    @Test
    void inheritedDetailAndMutationsRejectHiddenMultiTargetRecord() {
        ordinaryUser();
        SsePushRecordRepository repository = mock(SsePushRecordRepository.class);
        SsePushRecordServiceImpl service = new SsePushRecordServiceImpl(repository, mock(SseProperties.class));
        SsePushRecord restricted = record("user-2");
        restricted.setUserIds("user-2," + PlatformSuperAdmin.USER_ID);
        when(repository.entityClass()).thenReturn(SsePushRecord.class);
        when(repository.findById("record-1")).thenReturn(Optional.of(restricted));

        assertThatThrownBy(() -> service.detail("record-1"))
                .isInstanceOf(CoreBizException.class).hasMessageContaining("不存在");
        assertThatThrownBy(() -> service.update("record-1", mock(SsePushRecordDTO.class)))
                .isInstanceOf(CoreBizException.class).hasMessageContaining("不存在");
        assertThatThrownBy(() -> service.delete("record-1"))
                .isInstanceOf(CoreBizException.class).hasMessageContaining("不存在");
        assertThatThrownBy(() -> service.updateEnabled("record-1", true))
                .isInstanceOf(CoreBizException.class).hasMessageContaining("不存在");
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any(SsePushRecord.class));
    }

    private static SsePushRecord record(String userId) {
        SsePushRecord record = new SsePushRecord();
        record.setId("record-1");
        record.setUserId(userId);
        return record;
    }

    private static void ordinaryUser() {
        UserContextHolder.set(new UserContext(
                "user-1", "tenant-1", false, null,
                Set.of(), Set.of(), Set.of(), Map.of()));
    }
}
