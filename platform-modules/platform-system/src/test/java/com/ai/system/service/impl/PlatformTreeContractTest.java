package com.ai.system.service.impl;

import com.ai.system.domain.MenuType;
import com.ai.system.domain.entity.Dept;
import com.ai.system.domain.entity.Menu;
import com.ai.system.domain.entity.Region;
import com.ai.system.domain.vo.RegionVO;
import com.ai.system.repository.DeptRepository;
import com.ai.system.repository.MenuRepository;
import com.ai.system.repository.RegionRepository;
import com.ai.system.repository.RoleMenuRepository;
import com.ai.system.repository.UserRepository;
import com.ai.system.repository.UserRoleRepository;
import com.ai.system.security.AuthorizationCacheService;
import com.ai.system.service.IPostService;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformTreeContractTest {

    @Test
    void departmentTreePreservesInheritedAndBusinessFieldsWithoutPromotingOrphans() {
        DeptRepository repository = mock(DeptRepository.class);
        DeptServiceImpl service = new DeptServiceImpl(repository, mock(RegionRepository.class),
                mock(IPostService.class), mock(UserRepository.class));
        Dept root = department("root", null);
        root.setRemark("部门备注");
        root.setSortOrder(7);
        Dept child = department("child", "root");
        child.setDeptCode("D-01");
        child.setRegionCode("region-01");
        Dept orphan = department("orphan", "hidden-parent");
        when(repository.findByDeletedFalse()).thenReturn(List.of(child, root, orphan));

        var roots = service.tree();

        assertThat(roots).hasSize(1);
        var result = roots.getFirst();
        assertThat(result.getId()).isEqualTo("root");
        assertThat(result.getRemark()).isEqualTo("部门备注");
        assertThat(result.getSortOrder()).isEqualTo(7);
        assertThat(result.getChildren()).hasSize(1);
        assertThat(result.getChildren().getFirst().getDeptCode()).isEqualTo("D-01");
        assertThat(result.getChildren().getFirst().getRegionCode()).isEqualTo("region-01");
        assertThat(result.getChildren().getFirst().getChildren()).isEmpty();
    }

    @Test
    void departmentCycleFailsExplicitly() {
        DeptRepository repository = mock(DeptRepository.class);
        DeptServiceImpl service = new DeptServiceImpl(repository, mock(RegionRepository.class),
                mock(IPostService.class), mock(UserRepository.class));
        when(repository.findByDeletedFalse()).thenReturn(List.of(
                department("a", "b"), department("b", "a")));

        assertThatIllegalArgumentException().isThrownBy(service::tree);
    }

    @Test
    void regionCodeSelectsOnlyItsSubtreeAndPreservesTheExistingRootMarker() {
        RegionRepository repository = mock(RegionRepository.class);
        Converter converter = mock(Converter.class);
        RegionServiceImpl service = new RegionServiceImpl(repository, converter);
        Region rootEntity = new Region();
        rootEntity.setId("root");
        List<Region> entities = List.of(rootEntity);
        RegionVO root = region("root", "0", "region-01");
        RegionVO child = region("child", "root", "region-02");
        RegionVO unrelated = region("another-root", null, "region-03");
        when(repository.findByDeletedFalseOrderBySortOrderAsc()).thenReturn(entities);
        when(converter.convert(entities, RegionVO.class)).thenReturn(List.of(root, child, unrelated));
        when(repository.findByRegionCodeAndDeletedFalse("region-01")).thenReturn(Optional.of(rootEntity));
        when(converter.convert(rootEntity, RegionVO.class)).thenReturn(root);

        var roots = service.tree("region-01");

        assertThat(roots).containsExactly(root);
        assertThat(root.getParentId()).isEqualTo("0");
        assertThat(root.getChildren()).containsExactly(child);
        assertThat(child.getRegionCode()).isEqualTo("region-02");
        assertThat(child.getChildren()).isEmpty();
    }

    @Test
    void menuTreeKeepsOrderingAndTenantVisibilityBeforeAssembly() {
        MenuRepository repository = mock(MenuRepository.class);
        MenuServiceImpl service = new MenuServiceImpl(repository, mock(UserRoleRepository.class),
                mock(RoleMenuRepository.class), mock(AuthorizationCacheService.class), mock(ObjectProvider.class));
        Menu root = menu("root", "0", 0);
        Menu later = menu("later", "root", 2);
        Menu first = menu("first", "root", 1);
        Menu hidden = menu("tenant", null, 0);
        hidden.setPermission("system:tenant:list");
        Menu hiddenChild = menu("tenant-child", "tenant", 0);
        when(repository.findByDeletedFalse()).thenReturn(List.of(later, hiddenChild, root, first, hidden));

        var roots = service.tree();

        assertThat(roots).extracting(node -> node.getId()).containsExactly("root");
        assertThat(roots.getFirst().getChildren()).extracting(node -> node.getId())
                .containsExactly("first", "later");
    }

    private static Dept department(String id, String parentId) {
        Dept dept = new Dept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptName(id);
        return dept;
    }

    private static RegionVO region(String id, String parentId, String code) {
        RegionVO region = new RegionVO();
        region.setId(id);
        region.setParentId(parentId);
        region.setRegionCode(code);
        return region;
    }

    private static Menu menu(String id, String parentId, int order) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuName(id);
        menu.setMenuTitle(id);
        menu.setMenuType(MenuType.DIRECTORY);
        menu.setSortOrder(order);
        menu.setVisible(true);
        return menu;
    }
}
