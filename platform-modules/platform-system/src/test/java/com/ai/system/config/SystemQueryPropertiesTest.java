package com.ai.system.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 系统管理查询窗口配置测试。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
class SystemQueryPropertiesTest {

    @Test
    void shouldUseSafeDefault() {
        assertThat(SystemQueryProperties.defaults().optionLimit()).isEqualTo(100);
        assertThat(SystemQueryProperties.defaults().maintenanceBatchSize()).isEqualTo(500);
    }

    @Test
    void shouldRejectUnsafeLimit() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SystemQueryProperties(501, 500))
                .withMessage("系统选项数量必须在 1 到 500 之间");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SystemQueryProperties(100, 5_001))
                .withMessage("系统维护批量必须在 1 到 5000 之间");
    }
}
