package com.ai.system.provider;

import io.github.guanxiangkai.web.plus.core.model.DictItem;
import io.github.guanxiangkai.web.plus.core.spi.DictProvider;
import com.ai.system.domain.vo.DictItemVO;
import com.ai.system.service.IDictItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 系统字典 {@link DictProvider} 实现（L3 回源）
 * <p>
 * 当 Web Plus 三级缓存（L1 Caffeine + L2 Redis）均未命中时，
 * 由框架调用此 Bean 按字典代码从数据库加载字典项，并自动回填缓存。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 * @see SysDictWriter
 */
@Component
@Slf4j
public class SysDictProvider implements DictProvider {


    private final IDictItemService dictItemService;

    /**
     * 创建字典回源提供者。
     *
     * @param dictItemService 延迟解析的字典项服务，用于隔离 JPA Plus 初始化时序
     */
    public SysDictProvider(@Lazy IDictItemService dictItemService) {
        this.dictItemService = dictItemService;
    }

    @Override
    public List<DictItem> provide(String code) {
        try {
            List<DictItemVO> items = dictItemService.getByDictCode(code);
            if (items == null || items.isEmpty()) {
                return Collections.emptyList();
            }
            return items.stream()
                    .filter(vo -> !Boolean.FALSE.equals(vo.getEnabled()))
                    .map(vo -> new DictItem(vo.getItemValue(), vo.getItemLabel()))
                    .toList();
        } catch (Exception e) {
            log.error("[SysDictProvider] 加载字典项失败: dictCode={}", code, e);
            return Collections.emptyList();
        }
    }
}
