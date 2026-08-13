package com.ai.system.repository;

import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import com.ai.system.domain.entity.Post;
import com.ai.system.domain.vo.PostPageVO;
import com.ai.system.domain.vo.PostVO;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 岗位Repository
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Repository
public interface PostRepository extends BaseRepository<PostPageVO, PostVO, Post> {

    /**
     * 根据岗位编码查询（过滤逻辑删除）
     *
     * @param code 岗位编码
     * @return 岗位
     */
    Optional<Post> findByPostCodeAndDeletedFalse(String postCode);

    /** 查询可供选择的启用岗位（过滤逻辑删除）。 */
    List<Post> findByEnabledTrueAndDeletedFalse(Pageable pageable);

    /**
     * 检查岗位编码是否存在（过滤逻辑删除）
     *
     * @param code 岗位编码
     * @return 是否存在
     */
    boolean existsByPostCodeAndDeletedFalse(String postCode);
}
