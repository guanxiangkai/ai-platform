package com.ai.system.provider;

import io.github.guanxiangkai.web.plus.core.model.DictItem;
import io.github.guanxiangkai.web.plus.core.spi.DictWriteSink;
import io.github.guanxiangkai.web.plus.core.spi.DictWriter;
import com.ai.system.constants.SystemConstants;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统内置字典写入器。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Component
public class SystemBuiltinDictWriter implements DictWriter {

    @Override
    public void write(DictWriteSink sink) {
        sink.put(SystemConstants.DictTypeConstants.SYS_USER_TYPE, List.of(
                new DictItem(SystemConstants.UserConstants.TYPE_ADMIN, "管理员"),
                new DictItem(SystemConstants.UserConstants.TYPE_USER, "用户")
        ));
    }
}
