package com.ai.system.controller.internal;

import com.ai.system.domain.vo.DictVO;
import com.ai.system.service.IDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典内部接口（仅供微服务间 RPC 调用）
 * <p>
 * 路径前缀 {@code /internal/dict}，由网关白名单控制访问，不对外暴露。
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@RestController
@RequestMapping("/internal/dict")
@RequiredArgsConstructor
public class InternalDictController {

    private final IDictService dictService;

    /**
     * 根据字典类型查询字典项列表
     */
    @GetMapping("/getByType")
    public List<DictVO> getByType(@RequestParam("type") String type) {
        return dictService.getByType(type);
    }
}
