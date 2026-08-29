package com.ai.system.provider;

import lombok.extern.slf4j.Slf4j;

import io.github.guanxiangkai.jpa.plus.field.dict.model.DictTranslateItem;
import io.github.guanxiangkai.jpa.plus.field.dict.spi.DictProvider;
import io.github.guanxiangkai.redis.plus.cache.ThreeLevelCacheTemplate;
import io.github.guanxiangkai.redis.plus.cache.annotation.ThreeLevelCacheable;
import com.ai.system.config.SystemDictCacheProperties;
import com.ai.system.constants.SystemConstants;
import com.ai.system.domain.vo.DictItemVO;
import com.ai.system.service.IDictItemService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA Plus {@link DictProvider} SPI 实现
 * <p>
 * 从数据库加载字典项，通过三级缓存（ThreeLevelCacheTemplate）加速翻译。
 *
 * <h3>为什么字典服务使用 {@code @Lazy} 构造器注入？</h3>
 * <p>
 * 本类位于 JPA Plus 初始化链中：
 * <pre>
 *   JpaPlusExecutor → FieldEngine → DictFieldHandler → DictProvider（本类）
 * </pre>
 * 而字典服务依赖的 Repository 工厂 Bean {@code JpaPlusRepositoryFactoryBean}
 * 持有 {@code JpaPlusExecutor} 字段。若立即解析字典服务，Spring 启动时会实例化
 * {@code JpaPlusRepositoryFactoryBean}，从而形成循环依赖：
 * <pre>
 *   JpaPlusExecutor → ... → CacheDictProvider → IDictItemService → DictItemRepository
 *                                                  → JpaPlusRepositoryFactoryBean
 *                                                  → JpaPlusExecutor ✗
 * </pre>
 * 在构造器参数上使用 {@code @Lazy} 后，Spring 注入懒代理而不立即触发工厂 Bean 实例化。
 * 真正调用 Repository 方法时（运行期），{@code JpaPlusExecutor} 早已就绪，循环消除：
 * <pre>
 *   启动期：JpaPlusExecutor → ... → CacheDictProvider ← 字典服务懒代理 ✓
 *   运行期：懒代理 → IDictItemService → DictItemRepository → JpaPlusExecutor（已就绪）✓
 * </pre>
 * <p>
 * 依赖 {@link IDictItemService} 保持 SPI 与持久化实现解耦，并由 Spring 的懒代理隔离初始化时序。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Primary
@Component
@Slf4j
public class CacheDictProvider implements DictProvider {


    private final ObjectProvider<ThreeLevelCacheTemplate> cacheTemplateProvider;


    private final IDictItemService dictItemService;

    private final SystemDictCacheProperties dictCacheProperties;

    public CacheDictProvider(
            ObjectProvider<ThreeLevelCacheTemplate> cacheTemplateProvider,
            @Lazy IDictItemService dictItemService,
            SystemDictCacheProperties dictCacheProperties) {
        this.cacheTemplateProvider = cacheTemplateProvider;
        this.dictItemService = dictItemService;
        this.dictCacheProperties = dictCacheProperties;
    }

    @Override
    @ThreeLevelCacheable(name = SystemConstants.CacheConstants.DICT_CACHE_NAME, key = "#dictCode + ':items'")
    public List<DictTranslateItem> getItems(String dictCode) {
        return loadItems(dictCode);
    }

    @Override
    public Optional<String> getLabel(String type, Object value) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadLabelsWithCacheFallback(type).get(String.valueOf(value)));
    }

    /**
     * 刷新指定字典类型的缓存
     *
     * @param dictCode 字典代码
     */
    public void refresh(String dictCode) {
        try {
            ThreeLevelCacheTemplate cacheTemplate = requireCacheTemplate();
            cacheTemplate.evict(SystemConstants.CacheConstants.DICT_CACHE_NAME, dictCode);
            cacheTemplate.evict(SystemConstants.CacheConstants.DICT_CACHE_NAME, dictCode + ":items");
        } catch (Exception e) {
            log.error("刷新字典缓存失败: dictCode={}", dictCode, e);
        }
    }

    // ─────────────────────────── private ───────────────────────────────────

    private List<DictTranslateItem> loadItems(String dictCode) {
        try {
            List<DictItemVO> entities =
                    dictItemService.getByDictCode(dictCode);
            if (entities == null || entities.isEmpty()) {
                return Collections.emptyList();
            }
            return entities.stream()
                    .filter(e -> !Boolean.FALSE.equals(e.getEnabled()))
                    .map(e -> new DictTranslateItem(
                            e.getDictCode(),
                            e.getItemValue(),
                            e.getItemLabel(),
                            e.getItemStyle(),
                            e.getSortOrder()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("加载字典项数据失败: dictCode={}", dictCode, e);
            return Collections.emptyList();
        }
    }

    private Map<String, String> loadLabels(String dictCode) {
        try {
            List<DictItemVO> entities =
                    dictItemService.getByDictCode(dictCode);
            if (entities == null || entities.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, String> labels = new LinkedHashMap<>();
            for (DictItemVO  entity: entities) {
                if (Boolean.FALSE.equals(entity.getEnabled())) {
                    continue;
                }
                labels.put(entity.getItemValue(), entity.getItemLabel());
            }
            return labels;
        } catch (Exception e) {
            log.error("加载字典数据失败: dictCode={}", dictCode, e);
            return Collections.emptyMap();
        }
    }

    private Map<String, String> loadLabelsWithCacheFallback(String dictCode) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> labels = (Map<String, String>) requireCacheTemplate().get(
                    SystemConstants.CacheConstants.DICT_CACHE_NAME,
                    dictCode,
                    Map.class,
                    dictCacheProperties.ttl(),
                    k -> loadLabels(dictCode));
            return labels != null ? labels : Collections.emptyMap();
        } catch (Exception e) {
            log.debug("字典缓存读取失败，回退直接查询: dictCode={}", dictCode, e);
            return loadLabels(dictCode);
        }
    }

    private ThreeLevelCacheTemplate requireCacheTemplate() {
        ThreeLevelCacheTemplate cacheTemplate = cacheTemplateProvider.getIfAvailable();
        if (cacheTemplate == null) {
            throw new IllegalStateException("三级缓存模板未初始化");
        }
        return cacheTemplate;
    }
}
