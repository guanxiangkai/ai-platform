package com.ai.system.service.impl;

import io.github.guanxiangkai.jpa.plus.interceptor.permission.enums.DataScopeType;
import com.ai.system.domain.entity.Post;
import com.ai.system.domain.entity.Dept;
import com.ai.system.domain.entity.Region;
import com.ai.system.domain.entity.User;
import com.ai.system.repository.DeptRepository;
import com.ai.system.repository.RegionRepository;
import com.ai.system.repository.UserRepository;
import com.ai.system.service.IPostService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.ai.system.constants.TestConstants.DeptConstants.POST_DEPT_ID;
import static com.ai.system.constants.TestConstants.DeptConstants.USER_DEPT_ID;
import static com.ai.system.constants.TestConstants.DeptConstants.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeptServiceImplTest {

    @Test
    void getSelectedDeptShouldUsePostDeptBeforeUserDept() {
        DeptRepository deptRepository = mock(DeptRepository.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        IPostService postService = mock(IPostService.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeptServiceImpl service = new DeptServiceImpl(
                deptRepository,
                regionRepository,
                postService,
                userRepository
        );
        Dept postDept = new Dept();
        postDept.setId(POST_DEPT_ID);
        postDept.setDeptName("研发一组");

        when(postService.getSelectedDeptId(USER_ID)).thenReturn(POST_DEPT_ID);
        when(deptRepository.findByIdAndDeletedFalse(POST_DEPT_ID)).thenReturn(Optional.of(postDept));

        var selectedDept = service.getSelectedDept(USER_ID);

        assertThat(selectedDept).isNotNull();
        assertThat(selectedDept.getId()).isEqualTo(POST_DEPT_ID);
        assertThat(selectedDept.getDeptName()).isEqualTo("研发一组");
        verify(postService).getSelectedDeptId(USER_ID);
        verify(deptRepository).findByIdAndDeletedFalse(POST_DEPT_ID);
        verifyNoInteractions(userRepository);
    }

    @Test
    void getSelectedDeptIdShouldFallbackToUserDeptWhenPostDeptMissing() {
        DeptRepository deptRepository = mock(DeptRepository.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        IPostService postService = mock(IPostService.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeptServiceImpl service = new DeptServiceImpl(
                deptRepository,
                regionRepository,
                postService,
                userRepository
        );
        User user = new User();
        user.setDeptId(USER_DEPT_ID);

        when(postService.getSelectedDeptId(USER_ID)).thenReturn(null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThat(service.getSelectedDeptId(USER_ID)).isEqualTo(USER_DEPT_ID);
    }

    @Test
    void getDeptRegionShouldUseConfiguredDeptRegion() {
        DeptRepository deptRepository = mock(DeptRepository.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        IPostService postService = mock(IPostService.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeptServiceImpl service = new DeptServiceImpl(
                deptRepository,
                regionRepository,
                postService,
                userRepository
        );
        Dept dept = new Dept();
        dept.setId(POST_DEPT_ID);
        dept.setRegionCode("101190206");
        Region region = new Region();
        region.setId("region-binhu");
        region.setRegionCode("101190206");
        region.setRegionName("滨湖区");
        region.setFullName("示例市示例区");
        region.setRegionLevel("district");

        when(deptRepository.findByIdAndDeletedFalse(POST_DEPT_ID)).thenReturn(Optional.of(dept));
        when(regionRepository.findByRegionCodeAndDeletedFalse("101190206")).thenReturn(Optional.of(region));

        var result = service.getDeptRegion(POST_DEPT_ID);

        assertThat(result.getRegionCode()).isEqualTo("101190206");
        assertThat(result.getRegionName()).isEqualTo("滨湖区");
        assertThat(result.getFullName()).isEqualTo("示例市示例区");
    }

    @Test
    void getDeptRegionShouldFallbackToParentDeptRegion() {
        DeptRepository deptRepository = mock(DeptRepository.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        IPostService postService = mock(IPostService.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeptServiceImpl service = new DeptServiceImpl(
                deptRepository,
                regionRepository,
                postService,
                userRepository
        );
        Dept childDept = new Dept();
        childDept.setId(POST_DEPT_ID);
        childDept.setParentId("parent-dept");
        Dept parentDept = new Dept();
        parentDept.setId("parent-dept");
        parentDept.setRegionCode("101190206");
        Region region = new Region();
        region.setId("region-binhu");
        region.setRegionCode("101190206");
        region.setRegionName("滨湖区");

        when(deptRepository.findByIdAndDeletedFalse(POST_DEPT_ID)).thenReturn(Optional.of(childDept));
        when(deptRepository.findByIdAndDeletedFalse("parent-dept")).thenReturn(Optional.of(parentDept));
        when(regionRepository.findByRegionCodeAndDeletedFalse("101190206")).thenReturn(Optional.of(region));

        var result = service.getDeptRegion(POST_DEPT_ID);

        assertThat(result.getRegionCode()).isEqualTo("101190206");
    }

    @Test
    void getUserDeptIdsShouldUseSameSelectedDeptFallback() {
        DeptRepository deptRepository = mock(DeptRepository.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        IPostService postService = mock(IPostService.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeptServiceImpl service = new DeptServiceImpl(
                deptRepository,
                regionRepository,
                postService,
                userRepository
        );
        User user = new User();
        user.setDeptId(USER_DEPT_ID);
        Post post = new Post();
        post.setId("post-1");
        post.setDeptId(USER_DEPT_ID);
        post.setDataScope(DataScopeType.DEPT);

        when(postService.getSelectedDeptId(USER_ID)).thenReturn(null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(postService.getUserPostsByDept(USER_ID, USER_DEPT_ID)).thenReturn(List.of(post));

        Set<String> deptIds = service.getUserDeptIds(USER_ID, false);

        assertThat(deptIds).containsExactly(USER_DEPT_ID);
    }

    @Test
    void getUserDeptIdsShouldUnionSelectedDeptPostDataScopes() {
        DeptRepository deptRepository = mock(DeptRepository.class);
        RegionRepository regionRepository = mock(RegionRepository.class);
        IPostService postService = mock(IPostService.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeptServiceImpl service = new DeptServiceImpl(
                deptRepository,
                regionRepository,
                postService,
                userRepository
        );
        Post deptPost = new Post();
        deptPost.setId("post-1");
        deptPost.setDeptId(POST_DEPT_ID);
        deptPost.setDataScope(DataScopeType.DEPT);
        Post childScopePost = new Post();
        childScopePost.setId("post-2");
        childScopePost.setDeptId(POST_DEPT_ID);
        childScopePost.setDataScope(DataScopeType.DEPT_AND_CHILD);
        Dept childDept = new Dept();
        childDept.setId("child-dept-1");

        when(postService.getSelectedDeptId(USER_ID)).thenReturn(POST_DEPT_ID);
        when(postService.getUserPostsByDept(USER_ID, POST_DEPT_ID)).thenReturn(List.of(deptPost, childScopePost));
        when(deptRepository.findByParentIdAndDeletedFalse(POST_DEPT_ID)).thenReturn(List.of(childDept));
        when(deptRepository.findByParentIdAndDeletedFalse("child-dept-1")).thenReturn(List.of());

        Set<String> deptIds = service.getUserDeptIds(USER_ID, false);

        assertThat(deptIds).containsExactlyInAnyOrder(POST_DEPT_ID, "child-dept-1");
    }
}
