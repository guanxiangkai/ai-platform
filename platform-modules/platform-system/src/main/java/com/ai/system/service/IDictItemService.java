package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.DictItemCreateDTO;
import com.ai.system.domain.dto.DictItemDTO;
import com.ai.system.domain.dto.DictItemPageDTO;
import com.ai.system.domain.entity.DictItem;
import com.ai.system.domain.vo.DictItemPageVO;
import com.ai.system.domain.vo.DictItemVO;

import java.util.List;

/**
 * 字典项服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IDictItemService extends IBaseService<DictItemPageDTO, DictItemPageVO, DictItemVO, DictItemCreateDTO, DictItemDTO, DictItem> {

    /**
     * 获取字典项选项列表
     *
     * @param dictCode 字典类型
     * @return dictCode
     */
    List<OptionItem> options(String dictCode);

    /**
     * 根据字典ID查询字典项
     *
     * @param dictId 字典ID
     * @return 字典项列表
     */
    List<DictItemVO> getByDictId(String dictId);

    /**
     * 根据字典代码查询字典项
     *
     * @param dictCode 字典代码
     * @return 字典项列表
     */
    List<DictItemVO> getByDictCode(String dictCode);

    /**
     * 更新字典项排序
     *
     * @param ids 排序后的字典项ID列表
     * @return 是否成功
     */
    Boolean updateSort(List<String> ids);

    /**
     * 原子设置默认字典项（同 dictCode 下仅一个默认）
     *
     * @param id 字典项ID
     * @return 是否成功
     */
    Boolean setDefault(String id);

    Long getCountByDictId(String dictId);

    /**
     * 查询所有启用的字典项（供 {@code SysDictWriter} 启动预热使用）
     *
     * @return 全量启用字典项
     */
    List<DictItemVO> listAllEnabled();
}
