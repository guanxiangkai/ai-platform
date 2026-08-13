package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.DictCreateDTO;
import com.ai.system.domain.dto.DictDTO;
import com.ai.system.domain.dto.DictPageDTO;
import com.ai.system.domain.entity.Dict;
import com.ai.system.domain.vo.DictPageVO;
import com.ai.system.domain.vo.DictVO;

import java.util.List;

/**
 * 字典服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IDictService extends IBaseService<DictPageDTO, DictPageVO, DictVO, DictCreateDTO, DictDTO, Dict> {

    /**
     * 获取字典选项列表
     *
     * @return 字典选项列表
     */
    List<OptionItem> options();

    /**
     * 根据类型获取字典项
     *
     * @param type 字典类型
     * @return 字典项列表
     */
    List<DictVO> getByType(String type);

    /**
     * 更新字典排序
     *
     * @param ids 排序后的字典ID列表
     * @return 是否成功
     */
    Boolean updateSort(List<String> ids);

    /**
     * 刷新字典缓存
     *
     * @return 是否成功
     */
    Boolean refreshCache();
}
