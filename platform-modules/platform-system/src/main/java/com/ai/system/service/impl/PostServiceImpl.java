package com.ai.system.service.impl;

import com.ai.system.config.SystemQueryProperties;
import lombok.extern.slf4j.Slf4j;

import io.github.guanxiangkai.redis.plus.cache.ThreeLevelCacheTemplate;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.core.constants.AuthConstants;
import io.github.guanxiangkai.web.plus.core.model.OptionItem;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import com.ai.system.domain.dto.PostCreateDTO;
import com.ai.system.domain.dto.PostDTO;
import com.ai.system.domain.dto.PostPageDTO;
import com.ai.system.domain.entity.Dept;
import com.ai.system.domain.entity.Post;
import com.ai.system.domain.entity.UserPost;
import com.ai.system.domain.vo.PostPageVO;
import com.ai.system.domain.vo.PostVO;
import com.ai.system.repository.DeptRepository;
import com.ai.system.repository.PostRepository;
import com.ai.system.repository.UserPostRepository;
import com.ai.system.security.AuthUserCacheService;
import com.ai.system.service.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 岗位服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl extends BaseServiceImpl<PostPageDTO, PostPageVO, PostVO, PostCreateDTO, PostDTO, Post> implements IPostService {


    private final PostRepository repository;
    private final SystemQueryProperties queryProperties;
    private final UserPostRepository userPostRepository;
    private final ObjectProvider<ThreeLevelCacheTemplate> cacheTemplateProvider;
    private final ObjectProvider<AuthUserCacheService> authUserCacheServiceProvider;
    private final DeptRepository deptRepository;

    @Override
    protected BaseRepository<PostPageVO, PostVO, Post> getRepository() {
        return this.repository;
    }

    @Override
    public List<OptionItem> options() {
        return repository.findByEnabledTrueAndDeletedFalse(PageRequest.of(0, queryProperties.optionLimit())).stream()
                .map(post -> OptionItem.of(post.getPostName(), post.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(PostCreateDTO dto) {
        // 检查岗位编码是否重复
        if (StringUtils.hasText(dto.postCode()) &&
                repository.existsByPostCodeAndDeletedFalse(dto.postCode())) {
            throw new BizException("岗位编码已存在");
        }

        validateParentId(null, dto.parentId());

        return super.create(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, PostDTO dto) {
        PostVO existingVO = detail(id);
        Set<String> affectedUserIds = findUserIdsByPost(id);

        // 检查岗位编码是否重复（排除自己）
        if (StringUtils.hasText(dto.postCode()) &&
                !dto.postCode().equals(existingVO.getPostCode()) &&
                repository.existsByPostCodeAndDeletedFalse(dto.postCode())) {
            throw new BizException("岗位编码已存在");
        }

        validateParentId(id, dto.parentId());

        super.update(id, dto);
        refreshAuthUsers(affectedUserIds);
    }

    private void validateParentId(String currentId, String parentId) {
        if (!StringUtils.hasText(parentId)) {
            return;
        }
        if (StringUtils.hasText(currentId) && parentId.equals(currentId)) {
            throw new BizException("上级岗位不能是自己");
        }
        if (repository.findById(parentId).isEmpty()) {
            throw new BizException("上级岗位不存在");
        }
    }

    @Override
    public Boolean checkCode(String code) {
        return !repository.existsByPostCodeAndDeletedFalse(code);
    }

    @Override
    public List<Post> getUserPosts(String userId) {

        List<String> postIds = getActiveUserPostRelations(userId).stream()
                .map(UserPost::getPostId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        if (postIds.isEmpty()) {
            return List.of();
        }

        return repository.findAllById(postIds).stream()
                .filter(this::isActivePost)
                .toList();
    }

    @Override
    public List<Post> getUserPostsByDept(String userId, String deptId) {
        if (!StringUtils.hasText(deptId)) {
            return List.of();
        }
        return getUserPosts(userId).stream()
                .filter(post -> deptId.equals(post.getDeptId()))
                .toList();
    }

    @Override
    public Map<String, List<String>> getPostNamesByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserPost> userPosts = userPostRepository.findByUserIdIn(userIds).stream()
                .filter(userPost -> userPost.getEndDate() == null)
                .toList();
        List<String> allPostIds = userPosts.stream().map(UserPost::getPostId).distinct().toList();
        Map<String, String> postIdToName = repository.findAllById(allPostIds).stream()
                .filter(this::isActivePost)
                .collect(Collectors.toMap(Post::getId, Post::getPostName));
        return userPosts.stream()
                .collect(Collectors.groupingBy(
                        UserPost::getUserId,
                        Collectors.mapping(up -> postIdToName.getOrDefault(up.getPostId(), ""), Collectors.toList())
                ));
    }

    // ==================== 岗位选择 / 切换 ====================

    @Override
    public List<OptionItem> listMyPosts(String userId) {
        // 1. 查询用户所有有效岗位关系
        List<UserPost> userPosts = getActiveUserPostRelations(userId);
        if (userPosts.isEmpty()) {
            return List.of();
        }

        // 2. 批量查询关联的岗位信息
        List<String> postIds = userPosts.stream()
                .map(UserPost::getPostId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, Post> postMap = repository.findAllById(postIds).stream()
                .filter(this::isActivePost)
                .collect(Collectors.toMap(Post::getId, p -> p));

        // 3. 批量查询关联的部门名称
        List<String> deptIds = postMap.values().stream()
                .map(Post::getDeptId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, String> deptNameMap = deptRepository.findAllById(deptIds).stream()
                .collect(Collectors.toMap(Dept::getId, Dept::getDeptName));

        // 4. 获取当前选中的部门ID；岗位选择仅作为部门上下文入口，数据权限按该部门所有岗位并集计算。
        String selectedDeptId = getSelectedDeptId(userId);

        // 5. 组装 OptionItem（label=岗位名称, value=岗位ID, extra=附加信息）
        List<OptionItem> result = new ArrayList<>();
        for (UserPost up : userPosts) {
            Post post = postMap.get(up.getPostId());
            if (post == null) continue;

            HashMap<String, String> extra = new HashMap<>();
            extra.put("deptId", post.getDeptId() != null ? post.getDeptId() : "");
            extra.put("deptName", post.getDeptId() != null
                    ? deptNameMap.getOrDefault(post.getDeptId(), "") : "");
            extra.put("mainPost", String.valueOf(Boolean.TRUE.equals(up.getMainPost())));
            extra.put("selected", String.valueOf(post.getDeptId() != null && post.getDeptId().equals(selectedDeptId)));
            extra.put("postType", up.getPostType() != null ? up.getPostType() : "");

            result.add(OptionItem.of(post.getPostName(), post.getId(), extra));
        }
        return result;
    }

    @Override
    public void switchPost(String userId, String postId) {
        Post targetPost = repository.findById(postId)
                .filter(this::isActivePost)
                .orElseThrow(() -> new BizException("岗位不存在或已失效"));

        // 超级管理员可切换到任意岗位，普通用户只能切换到自己有效的岗位
        if (!SecurityUtils.isSuperAdmin()) {
            userPostRepository.findByUserIdAndPostIdAndEndDateIsNullAndDeletedFalse(userId, postId)
                    .orElseThrow(() -> new BizException("无权切换到该岗位或岗位已失效"));
        }

        // 写入三级缓存
        requireCacheTemplate().put(AuthConstants.PostConstants.SELECTED_POST_CACHE, userId, postId, AuthConstants.PostConstants.SELECTED_POST_TTL);
        AuthUserCacheService authUserCacheService = authUserCacheServiceProvider.getIfAvailable();
        if (authUserCacheService != null) {
            authUserCacheService.refreshAfterCommit(userId, null, true);
        }
        log.info("用户 {} 切换岗位为 {}", userId, targetPost.getId());
    }

    @Override
    public String getSelectedPostId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        // 从三级缓存获取；缓存未命中时调用 loader 从 DB 加载并回填
        return requireCacheTemplate().get(AuthConstants.PostConstants.SELECTED_POST_CACHE, userId, String.class, AuthConstants.PostConstants.SELECTED_POST_TTL,
                k -> loadSelectedPostId(k));
    }

    /**
     * 从数据库加载用户选中岗位（三级缓存的 L3 回源逻辑）
     */
    private String loadSelectedPostId(String userId) {
        List<UserPost> activeRelations = getActiveUserPostRelations(userId);
        if (activeRelations.isEmpty()) {
            return null;
        }

        Map<String, Post> postMap = repository.findAllById(activeRelations.stream()
                        .map(UserPost::getPostId)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList())
                .stream()
                .filter(this::isActivePost)
                .collect(Collectors.toMap(Post::getId, p -> p));
        if (postMap.isEmpty()) {
            return null;
        }

        return activeRelations.stream()
                .filter(userPost -> Boolean.TRUE.equals(userPost.getMainPost()))
                .map(UserPost::getPostId)
                .filter(postMap::containsKey)
                .findFirst()
                .or(() -> activeRelations.stream()
                        .map(UserPost::getPostId)
                        .filter(postMap::containsKey)
                        .findFirst())
                .orElse(null);
    }

    @Override
    public String getSelectedDeptId(String userId) {
        String postId = getSelectedPostId(userId);
        if (!StringUtils.hasText(postId)) {
            return null;
        }
        return getUserPosts(userId).stream()
                .filter(post -> postId.equals(post.getId()))
                .findFirst()
                .map(Post::getDeptId)
                .orElse(null);
    }

    @Override
    public List<String> getUserPostCodes(String userId) {
        return getUserPosts(userId).stream()
                .map(Post::getPostCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    @Override
    public List<String> getUserPostCodes(String userId, String deptId) {
        return getUserPostsByDept(userId, deptId).stream()
                .map(Post::getPostCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private List<UserPost> getActiveUserPostRelations(String userId) {
        if (!StringUtils.hasText(userId)) {
            return List.of();
        }
        return userPostRepository.findByUserIdAndEndDateIsNullAndDeletedFalse(userId);
    }

    private boolean isActivePost(Post post) {
        return post != null
                && !Boolean.TRUE.equals(post.getDeleted())
                && !Boolean.FALSE.equals(post.getEnabled());
    }

    private Set<String> findUserIdsByPost(String postId) {
        return userPostRepository.findByPostId(postId).stream()
                .map(UserPost::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void refreshAuthUsers(Set<String> userIds) {
        AuthUserCacheService authUserCacheService = authUserCacheServiceProvider.getIfAvailable();
        if (authUserCacheService != null) {
            authUserCacheService.refreshUsersAfterCommit(userIds, true);
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
