package com.ai.system.controller;

import io.github.guanxiangkai.web.plus.log.annotation.OperationLog;
import io.github.guanxiangkai.web.plus.web.controller.BaseController;
import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.constants.OperationTypes;
import io.github.guanxiangkai.web.plus.core.model.ApiResponse;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import com.ai.system.domain.dto.PostCreateDTO;
import com.ai.system.domain.dto.PostDTO;
import com.ai.system.domain.dto.PostPageDTO;
import com.ai.system.domain.entity.Post;
import com.ai.system.domain.vo.PostPageVO;
import com.ai.system.domain.vo.PostVO;
import com.ai.system.service.IPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理控制器岗位
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "岗位管理", description = "岗位的增删改查功能")
@RestController
@RequestMapping("/system/post")
@RequiredArgsConstructor
public class PostController extends BaseController<PostPageDTO, PostPageVO, PostVO, PostCreateDTO, PostDTO, Post> {

    private final IPostService service;

    @Override
    protected IBaseService<PostPageDTO, PostPageVO, PostVO, PostCreateDTO, PostDTO, Post> getService() {
        return this.service;
    }

    /**
     * 获取岗位选项列表
     *
     * @return 岗位选项列表
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '选项列表'}")
    @Operation(summary = "获取岗位选项列表", description = "获取所有岗位的选项列表，用于下拉选择等场景。")
    @GetMapping("/options")
    public ApiResponse<List<OptionItem>> options() {
        return ApiResponse.ok(service.options());
    }

    /**
     * 检查岗位编码是否可用
     *
     * @param code 岗位编码
     * @return 编码是否可用
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "#{getEntityName() + '编码检查'}")
    @Operation(summary = "检查岗位编码", description = "检查岗位编码是否已被使用。")
    @GetMapping("/check")
    public ApiResponse<Boolean> checkCode(@RequestParam String code) {
        return ApiResponse.ok(service.checkCode(code));
    }

    /**
     * 获取当前用户的岗位列表（用于岗位切换下拉）
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.QUERY, module = "#{getModuleName()}", description = "我的岗位列表")
    @Operation(summary = "我的岗位列表", description = "获取当前用户所有有效岗位，含部门名称、是否选中等信息。")
    @GetMapping("/my-posts")
    public ApiResponse<List<OptionItem>> myPosts() {
        return ApiResponse.ok(service.listMyPosts(SecurityUtils.getUserId()));
    }

    /**
     * 切换岗位
     *
     * @param postId 目标岗位ID
     */
    @OperationLog(entity = com.ai.api.system.log.PlatformOperationLog.class, typeCode = OperationTypes.UPDATE, module = "#{getModuleName()}", description = "切换岗位")
    @Operation(summary = "切换岗位", description = "切换当前用户的活跃岗位，影响当前部门和数据权限范围。")
    @PostMapping("/switch/{postId}")
    public ApiResponse<Void> switchPost(@PathVariable String postId) {
        service.switchPost(SecurityUtils.getUserId(), postId);
        return ApiResponse.ok();
    }


}
