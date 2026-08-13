package com.ai.system.service.impl;

import com.ai.system.config.SystemQueryProperties;
import lombok.extern.slf4j.Slf4j;

import io.github.guanxiangkai.web.plus.core.converter.EntityConverter;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import io.github.guanxiangkai.web.plus.dict.DictRefresher;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.DictCreateDTO;
import com.ai.system.domain.dto.DictDTO;
import com.ai.system.domain.dto.DictPageDTO;
import com.ai.system.domain.entity.Dict;
import com.ai.system.domain.vo.DictPageVO;
import com.ai.system.domain.vo.DictVO;
import com.ai.system.provider.CacheDictProvider;
import com.ai.system.repository.DictRepository;
import com.ai.system.service.IDictItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DictServiceImpl extends BaseServiceImpl<DictPageDTO, DictPageVO, DictVO, DictCreateDTO, DictDTO, Dict> implements com.ai.system.service.IDictService {


    private final DictRepository repository;
    private final IDictItemService dictItemService;
    private final CacheDictProvider cacheDictProvider;
    private final ObjectProvider<DictRefresher> dictRefresherProvider;
    private final SystemQueryProperties queryProperties;

    @Override
    protected BaseRepository<DictPageVO, DictVO, Dict> getRepository() {
        return this.repository;
    }

    @Override
    public PageResponse<DictPageVO> list(DictPageDTO pageDTO) {
        PageResponse<DictPageVO> page = super.list(pageDTO);
        if (page == null) {
            return null;
        }

        List<DictPageVO> records = page.records().stream().map(vo -> new DictPageVO(
                vo.getId(), vo.getEnabled(), vo.getSortOrder(), vo.getDictType(), vo.getDictLabel(), vo.getDictValue(),
                vo.getRemark(), dictItemService.getCountByDictId(vo.getId()), vo.getCreateTime()
        )).toList();
        return PageResponse.of(records, page.total(), page.pageNum(), page.pageSize());
    }

    @Override
    public List<OptionItem> options() {
        return repository.findByEnabledTrueAndDeletedFalse(PageRequest.of(0, queryProperties.optionLimit())).stream()
                .map(dict -> OptionItem.of(dict.getDictLabel(), dict.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DictVO> getByType(String type) {
        List<Dict> dicts = repository.findByDictTypeAndEnabledAndDeletedFalse(type, Boolean.TRUE);
        return dicts.stream()
                .map(dict -> EntityConverter.toVo(dict, DictVO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSort(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (int i = 0; i < ids.size(); i++) {
            final int sortOrder = i;
            repository.findById(ids.get(i)).ifPresent(dict -> {
                dict.setSortOrder(sortOrder);
                repository.save(dict);
            });
        }
        log.info("字典排序更新完成，共处理 {} 条", ids.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean refreshCache() {
        log.info("开始刷新全量字典缓存");
        DictRefresher dictRefresher = dictRefresherProvider.getIfAvailable();
        if (dictRefresher != null) {
            dictRefresher.invalidateAll();
            dictRefresher.refresh();
            log.info("全量字典缓存刷新完成（Web Plus DictRefresher）");
        }
        Pageable pageable = PageRequest.of(
                0,
                queryProperties.maintenanceBatchSize(),
                Sort.by(Sort.Direction.ASC, "id"));
        int refreshedCount = 0;
        Page<Dict> page;
        do {
            page = repository.findByDeletedFalse(pageable);
            for (Dict dict : page.getContent()) {
                String dictCode = dict.getDictType();
                try {
                    cacheDictProvider.refresh(dictCode);
                    cacheDictProvider.getLabel(dictCode, "");
                    log.debug("字典缓存已刷新并预热: dictCode={}", dictCode);
                } catch (Exception e) {
                    log.error("刷新字典缓存失败: dictCode={}", dictCode, e);
                }
            }
            refreshedCount += page.getNumberOfElements();
            pageable = page.nextPageable();
        } while (page.hasNext());
        log.info("全量字典缓存刷新完成，共处理 {} 个字典", refreshedCount);
        return true;
    }

    @Override
    protected void afterCreate(Dict entity, DictCreateDTO dto) {
        refreshDictCache(entity.getDictType());
    }

    @Override
    protected void afterUpdate(Dict entity, DictDTO dto) {
        refreshDictCache(entity.getDictType());
    }

    @Override
    protected void afterDelete(Dict entity) {
        refreshDictCache(entity.getDictType());
    }

    private void refreshDictCache(String dictType) {
        DictRefresher dictRefresher = dictRefresherProvider.getIfAvailable();
        if (dictRefresher != null && dictType != null) {
            try {
                dictRefresher.invalidate(dictType);
                dictRefresher.refresh();
            } catch (Exception e) {
                log.warn("[DictServiceImpl] Web Plus 字典缓存刷新失败: dictType={}", dictType, e);
            }
        }
        if (dictType != null) {
            cacheDictProvider.refresh(dictType);
        }
    }
}
