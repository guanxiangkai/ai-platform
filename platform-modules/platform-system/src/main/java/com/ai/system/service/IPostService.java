package com.ai.system.service;

import io.github.guanxiangkai.web.plus.web.service.IBaseService;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import com.ai.system.domain.dto.PostCreateDTO;
import com.ai.system.domain.dto.PostDTO;
import com.ai.system.domain.dto.PostPageDTO;
import com.ai.system.domain.entity.Post;
import com.ai.system.domain.vo.PostPageVO;
import com.ai.system.domain.vo.PostVO;

import java.util.List;
import java.util.Map;

/**
 * 岗位服务接口
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public interface IPostService extends IBaseService<PostPageDTO, PostPageVO, PostVO, PostCreateDTO, PostDTO, Post> {

    /**
     * 获取岗位选项列表
     *
     * @return 岗位选项列表
     */
    List<OptionItem> options();

    /**
     * 检查岗位编码是否可用
     *
     * @param code 岗位编码
     * @return 是否可用
     */
    Boolean checkCode(String code);

    /**
     * 获取用户的所有岗位信息
     *
     * @param userId 用户ID
     * @return 岗位列表
     */
    List<Post> getUserPosts(String userId);

    /**
     * 获取用户在指定部门下的有效岗位信息。
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 岗位列表
     */
    List<Post> getUserPostsByDept(String userId, String deptId);

    /**
     * 批量获取用户的岗位名称（userId → List&lt;postName&gt;）
     *
     * @param userIds 用户ID列表
     * @return userId 到岗位名列表的映射
     */
    Map<String, List<String>> getPostNamesByUserIds(List<String> userIds);

    /**
     * 获取用户的岗位选择列表
     * <p>
     * 返回 {@link OptionItem}：label=岗位名称，value=岗位ID，
     * extra 含 deptId、deptName、isPrimary、selected 等
     * </p>
     *
     * @param userId 用户ID
     * @return 岗位选项列表
     */
    List<OptionItem> listMyPosts(String userId);

    /**
     * 切换岗位：将选中的岗位写入 Redis（永不过期）
     *
     * @param userId 用户ID
     * @param postId 目标岗位ID
     */
    void switchPost(String userId, String postId);

    /**
     * 获取用户当前选中的岗位ID
     * <p>
     * 优先从 Redis 读取，不存在则取主岗位（isPrimary=true），同时写入 Redis
     * </p>
     *
     * @param userId 用户ID
     * @return 选中的岗位ID，无岗位返回 null
     */
    String getSelectedPostId(String userId);

    /**
     * 获取用户当前选中岗位的部门ID
     *
     * @param userId 用户ID
     * @return 部门ID，无岗位返回 null
     */
    String getSelectedDeptId(String userId);

    /**
     * 获取指定用户的岗位编码列表
     */
    List<String> getUserPostCodes(String userId);

    /**
     * 获取指定用户在指定部门下的岗位编码列表。
     */
    List<String> getUserPostCodes(String userId, String deptId);
}
