package com.ai.system.provider;

import io.github.guanxiangkai.web.plus.core.model.DictItem;
import com.ai.api.context.TenantExecutionScope;
import io.github.guanxiangkai.web.plus.core.spi.DictWriteSink;
import io.github.guanxiangkai.web.plus.core.spi.DictWriter;
import io.github.guanxiangkai.web.plus.core.util.SecurityUtils;
import com.ai.system.domain.vo.DictItemVO;
import com.ai.system.service.IDictItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统字典 {@link DictWriter} 实现
 * <p>
 * 应用启动就绪（{@code ApplicationReadyEvent}）后，由 {@code DictRefresher} 调用，
 * 将数据库中所有启用的字典项按字典代码（dictCode）分组后批量写入 Redis 三级缓存。
 * <br>
 * 配合 {@link SysDictProvider}（拉模式）使用，可兜底缓存未命中时的 L3 回源。
 * </p>
 *
 * <p><b>循环依赖说明：</b>与 {@code CacheDictProvider} 相同，通过 {@code @Lazy} 字段注入
 * 避免 {@code JpaPlusRepositoryFactoryBean} → {@code JpaPlusExecutor} 循环。</p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 * @see SysDictProvider
 */
@Component
@Slf4j
public class SysDictWriter implements DictWriter {


    @Lazy
    @Autowired
    private IDictItemService dictItemService;

    @Override
    public void write(DictWriteSink sink) {
        if (!StringUtils.hasText(SecurityUtils.getTenantId())
                && !StringUtils.hasText(TenantExecutionScope.currentTenantId())) {
            log.info("[SysDictWriter] 当前无租户作用域，跳过启动预热，字典将在租户首次访问时回源");
            return;
        }
        try {
            List<DictItemVO> allItems = dictItemService.listAllEnabled();
            if (allItems == null || allItems.isEmpty()) {
                log.info("[SysDictWriter] 无可用字典项，跳过缓存预热");
                return;
            }
            Map<String, List<DictItem>> grouped = allItems.stream()
                    .collect(Collectors.groupingBy(
                            DictItemVO::getDictCode,
                            Collectors.mapping(
                                    vo -> new DictItem(vo.getItemValue(), vo.getItemLabel()),
                                    Collectors.toList()
                            )
                    ));
            writeAll(sink, grouped);
            log.info("[SysDictWriter] 字典缓存预热完成，共 {} 种类型，{} 条记录",
                    grouped.size(), allItems.size());
        } catch (Exception e) {
            log.error("[SysDictWriter] 字典缓存预热失败", e);
        }
    }
}
