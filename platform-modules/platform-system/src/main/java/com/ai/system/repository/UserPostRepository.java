package com.ai.system.repository;

import io.github.guanxiangkai.jpa.plus.starter.repository.JpaPlusRepository;
import com.ai.system.domain.entity.UserPost;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户岗位关系Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface UserPostRepository extends JpaPlusRepository<UserPost, String> {

    /**
     * 根据用户ID查询岗位关系
     */
    default List<UserPost> findByUserId(String userId) {
        return findByUserIdAndDeletedFalse(userId);
    }

    /**
     * 根据岗位ID查询用户关系
     */
    default List<UserPost> findByPostId(String postId) {
        return findByPostIdAndDeletedFalse(postId);
    }

    /**
     * 根据用户ID删除关系
     */
    default void deleteByUserId(String userId) {
        hardDeleteByUserId(userId);
    }

    /**
     * 根据岗位ID删除关系
     */
    default void deleteByPostId(String postId) {
        hardDeleteByPostId(postId);
    }

    /**
     * 根据用户ID和岗位ID查询关系
     */
    default UserPost findByUserIdAndPostId(String userId, String postId) {
        return findByUserIdAndPostIdAndDeletedFalse(userId, postId);
    }

    /**
     * 根据用户ID列表查询岗位关系
     */
    default List<UserPost> findByUserIdIn(List<String> userIds) {
        return findByUserIdInAndDeletedFalse(userIds);
    }

    List<UserPost> findByUserIdAndDeletedFalse(String userId);

    List<UserPost> findByPostIdAndDeletedFalse(String postId);

    UserPost findByUserIdAndPostIdAndDeletedFalse(String userId, String postId);

    List<UserPost> findByUserIdInAndDeletedFalse(List<String> userIds);

    /**
     * 查询用户的主岗位（mainPost=true 且当前有效）
     */
    Optional<UserPost> findFirstByUserIdAndMainPostTrueAndEndDateIsNullAndDeletedFalse(String userId);

    /**
     * 查询用户所有有效的岗位关系（endDate 为空表示当前有效）
     */
    List<UserPost> findByUserIdAndEndDateIsNullAndDeletedFalse(String userId);

    /**
     * 查询用户指定岗位的有效关系
     */
    Optional<UserPost> findByUserIdAndPostIdAndEndDateIsNullAndDeletedFalse(String userId, String postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM sys_user_post WHERE user_id = :userId", nativeQuery = true)
    void hardDeleteByUserId(@Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM sys_user_post WHERE post_id = :postId", nativeQuery = true)
    void hardDeleteByPostId(@Param("postId") String postId);
}
