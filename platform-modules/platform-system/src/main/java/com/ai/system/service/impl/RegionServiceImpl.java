package com.ai.system.service.impl;

import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.core.tree.TreeAssembler;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import com.ai.system.domain.dto.RegionDTO;
import com.ai.system.domain.dto.RegionPageDTO;
import com.ai.system.domain.entity.Region;
import com.ai.system.domain.vo.RegionPageVO;
import com.ai.system.domain.vo.RegionVO;
import com.ai.system.repository.RegionRepository;
import com.ai.system.service.IRegionService;
import io.github.linpeilie.Converter;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 行政区域服务实现
 * <p>
 * 支持树形结构查询（省 → 市 → 区/县 → 街道）
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegionServiceImpl extends BaseServiceImpl<RegionPageDTO, RegionPageVO, RegionVO, RegionDTO, RegionDTO, Region>
        implements IRegionService {


    private final RegionRepository repository;
    private final Converter converter;

    @Override
    protected BaseRepository<RegionPageVO, RegionVO, Region> getRepository() {
        return this.repository;
    }

    @Override
    protected Specification<Region> buildQuerySpec(RegionPageDTO pageDTO) {
        if (pageDTO == null) {
            return null;
        }
        return SpecUtils.<Region>builder()
                .eqIfPresent(Region::getRegionCode, pageDTO.getRegionCode())
                .likeIfPresent(Region::getRegionName, pageDTO.getRegionName())
                .eqIfPresent(Region::getParentId, pageDTO.getParentId())
                .eqIfPresent(Region::getRegionLevel, pageDTO.getRegionLevel())
                .build();
    }

    @Override
    public List<RegionVO> tree(String code) {
        List<RegionVO> allRegions = converter.convert(
                repository.findByDeletedFalseOrderBySortOrderAsc(), RegionVO.class);
        if (!StringUtils.hasText(code)) {
            return buildTree(allRegions, null);
        }

        Region region = repository.findByRegionCodeAndDeletedFalse(code)
                .orElseThrow(() -> new BizException("区域不存在：" + code));
        RegionVO root = converter.convert(region, RegionVO.class);
        root.setChildren(buildTree(allRegions, root.getId()));
        return List.of(root);
    }

    @Override
    public List<OptionItem> children(String code) {
        if (!StringUtils.hasText(code)) {
            return repository.findByDeletedFalseOrderBySortOrderAsc().stream()
                    .map(region -> OptionItem.of(region.getRegionName(), region.getRegionCode()))
                    .collect(Collectors.toList());
        }

        String resolvedParentId = repository.findByRegionCodeAndDeletedFalse(code)
                .map(Region::getId)
                .orElseThrow(() -> new BizException("区域不存在：" + code));
        List<Region> children = repository.findByParentIdAndDeletedFalseOrderBySortOrderAsc(resolvedParentId);
        return children.stream()
                .map(region -> OptionItem.of(region.getRegionName(), region.getRegionCode()))
                .collect(Collectors.toList());
    }

    @Override
    public RegionVO getByCode(String code) {
        Region region = repository.findByRegionCodeAndDeletedFalse(code)
                .orElseThrow(() -> new BizException("区域不存在：" + code));
        return converter.convert(region, RegionVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(RegionDTO dto) {
        // 校验编码唯一性
        if (repository.existsByRegionCodeAndDeletedFalse(dto.regionCode())) {
            throw new BizException("区域编码已存在：" + dto.regionCode());
        }

        Region region = converter.convert(dto, Region.class);

        // 自动拼接 fullName
        if (StringUtils.hasText(dto.parentId()) && !"0".equals(dto.parentId())) {
            repository.findById(dto.parentId()).ifPresent(parent -> {
                String parentFullName = StringUtils.hasText(parent.getFullName())
                        ? parent.getFullName() : parent.getRegionName();
                region.setFullName(parentFullName + "/" + dto.regionName());
            });
        } else {
            region.setFullName(dto.regionName());
        }

        Region saved = repository.save(region);
        return saved.getId();
    }

    /**
     * 构建树形结构。
     */
    private List<RegionVO> buildTree(List<RegionVO> regions, String parentId) {
        return TreeAssembler.assemble(regions, candidate -> isParentMatch(parentId, candidate));
    }

    private boolean isParentMatch(String expectedParentId, String actualParentId) {
        if (!StringUtils.hasText(expectedParentId)) {
            return !StringUtils.hasText(actualParentId) || "0".equals(actualParentId);
        }
        return expectedParentId.equals(actualParentId);
    }

}
