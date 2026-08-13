package com.ai.system.service.impl;

import lombok.extern.slf4j.Slf4j;

import io.github.guanxiangkai.web.plus.core.converter.EntityConverter;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.dict.DictRefresher;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import com.ai.system.domain.dto.DictItemCreateDTO;
import com.ai.system.domain.dto.DictItemDTO;
import com.ai.system.domain.dto.DictItemPageDTO;
import com.ai.system.domain.entity.DictItem;
import com.ai.system.domain.vo.DictItemPageVO;
import com.ai.system.domain.vo.DictItemVO;
import com.ai.system.provider.CacheDictProvider;
import com.ai.system.repository.DictItemRepository;
import com.ai.system.service.IDictItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典项服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DictItemServiceImpl extends BaseServiceImpl<DictItemPageDTO, DictItemPageVO, DictItemVO, DictItemCreateDTO, DictItemDTO, DictItem> implements IDictItemService {


    private final DictItemRepository repository;
    private final ObjectProvider<DictRefresher> dictRefresherProvider;
    private final CacheDictProvider cacheDictProvider;

    @Override
    protected BaseRepository<DictItemPageVO, DictItemVO, DictItem> getRepository() {
        return this.repository;
    }

    @Override
    public List<OptionItem> options(String dictCode) {
        List<DictItemVO> dictItemList = this.getByDictCode(dictCode);
        return dictItemList.stream()
                .map(vo -> {
                    HashMap<String, String> extra = new HashMap<>();
                    extra.put("itemStyle", vo.getItemStyle() == null ? "" : vo.getItemStyle());
                    extra.put("itemColor", vo.getItemColor() == null ? "" : vo.getItemColor());
                    extra.put("itemCssClass", vo.getItemCssClass() == null ? "" : vo.getItemCssClass());
                    extra.put("itemSelected", String.valueOf(vo.getItemSelected()));
                    return OptionItem.of(vo.getItemLabel(), vo.getItemValue(), extra);
                })
                .collect(Collectors.toList());
    }

    @Override
    protected Specification<DictItem> buildQuerySpec(DictItemPageDTO pageDTO) {
        if (pageDTO == null) return (root, query, cb) -> cb.conjunction();
        return SpecUtils.<DictItem>builder()
                .eqIfPresent(DictItem::getDictId, pageDTO.getDictId())
                .eqIfPresent(DictItem::getDictCode, pageDTO.getDictCode())
                .likeIfPresent(DictItem::getItemLabel, pageDTO.getItemLabel())
                .eqIfPresent(DictItem::getEnabled, pageDTO.getEnabled())
                .eqIfPresent(DictItem::getItemSelected, pageDTO.getItemSelected())
                .build();
    }

    @Override
    public List<DictItemVO> getByDictId(String dictId) {
        return EntityConverter.toVoList(
                repository.findByDictId(dictId), DictItemVO.class);
    }

    @Override
    public List<DictItemVO> getByDictCode(String dictCode) {
        return EntityConverter.toVoList(
                repository.findByDictCode(dictCode), DictItemVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSort(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        Set<String> changedDictCodes = new HashSet<>();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            int sortOrder = i;
            repository.findById(id).ifPresent(item -> {
                item.setSortOrder(sortOrder);
                repository.save(item);
                if (StringUtils.hasText(item.getDictCode())) {
                    changedDictCodes.add(item.getDictCode());
                }
            });
        }

        changedDictCodes.forEach(this::refreshDictCache);
        log.info("字典项排序更新完成，共处理 {} 条", ids.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setDefault(String id) {
        DictItem target = repository.findById(id)
                .orElseThrow(() -> new BizException("字典项不存在"));
        if (Boolean.TRUE.equals(target.getDeleted())) {
            throw new BizException("字典项不存在");
        }
        if (!StringUtils.hasText(target.getDictCode())) {
            throw new BizException("字典项未关联字典代码，无法设置默认项");
        }
        repository.clearDefaultByDictCode(target.getDictCode());
        repository.setDefaultById(id);
        refreshDictCache(target.getDictCode());
        return true;
    }

    @Override
    protected void afterCreate(DictItem entity, DictItemCreateDTO dto) {
        normalizeSelection(entity);
        refreshDictCache(entity.getDictCode());
    }

    @Override
    protected void afterUpdate(DictItem entity, DictItemDTO dto) {
        normalizeSelection(entity);
        refreshDictCache(entity.getDictCode());
    }

    @Override
    protected void afterDelete(DictItem entity) {
        refreshDictCache(entity.getDictCode());
    }

    private void normalizeSelection(DictItem current) {
        if (current == null || !Boolean.TRUE.equals(current.getItemSelected())) {
            return;
        }
        String dictCode = current.getDictCode();
        if (!StringUtils.hasText(dictCode)) {
            return;
        }
        List<DictItem> sameCodeItems = repository.findByDictCode(dictCode);
        List<DictItem> toUpdate = new ArrayList<>();
        for (DictItem item : sameCodeItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            if (!item.getId().equals(current.getId()) && Boolean.TRUE.equals(item.getItemSelected())) {
                item.setItemSelected(false);
                toUpdate.add(item);
            }
        }
        if (!toUpdate.isEmpty()) {
            repository.saveAll(toUpdate);
        }
    }

    @Override
    public Long getCountByDictId(String dictId) {
        if (!StringUtils.hasText(dictId)) {
            return repository.countAll();
        }
        return repository.countByDictId(dictId);
    }

    @Override
    public List<DictItemVO> listAllEnabled() {
        return EntityConverter.toVoList(
                repository.findEnabledAll(), DictItemVO.class);
    }

    private void refreshDictCache(String dictCode) {
        if (!StringUtils.hasText(dictCode)) {
            return;
        }
        DictRefresher dictRefresher = dictRefresherProvider.getIfAvailable();
        if (dictRefresher != null) {
            try {
                dictRefresher.invalidate(dictCode);
                dictRefresher.refresh();
            } catch (Exception e) {
                log.warn("[DictItemServiceImpl] Web Plus 字典缓存刷新失败: dictCode={}", dictCode, e);
            }
        }
        cacheDictProvider.refresh(dictCode);
    }
}
