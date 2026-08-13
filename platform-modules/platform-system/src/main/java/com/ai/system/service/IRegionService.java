package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.RegionDTO;
import com.ai.system.domain.dto.RegionPageDTO;
import com.ai.system.domain.entity.Region;
import com.ai.system.domain.vo.RegionPageVO;
import com.ai.system.domain.vo.RegionVO;

import java.util.List;

/**
 * 行政区域服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IRegionService extends IBaseService<RegionPageDTO, RegionPageVO, RegionVO, RegionDTO, RegionDTO, Region> {

    /**
     * 查询区域树（完整树或指定编码子树）
     *
     * @param code 区域编码（为空时返回完整树）
     * @return 区域树
     */
    List<RegionVO> tree(String code);

    /**
     * 查询指定父区域编码下的子区域下拉数据
     *
     * @param code 父区域编码（为空时查询全部）
     * @return 子区域下拉列表
     */
    List<OptionItem> children(String code);

    /**
     * 根据区域编码查询
     *
     * @param code 区域编码
     * @return 区域VO
     */
    RegionVO getByCode(String code);
}
