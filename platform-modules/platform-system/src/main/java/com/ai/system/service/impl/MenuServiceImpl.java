package com.ai.system.service.impl;

import io.github.guanxiangkai.web.plus.core.converter.EntityConverter;
import io.github.guanxiangkai.web.plus.core.model.PageResponse;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import io.github.guanxiangkai.web.plus.web.repository.BaseRepository;
import io.github.guanxiangkai.web.plus.web.service.impl.BaseServiceImpl;
import io.github.guanxiangkai.web.plus.web.util.SpecUtils;
import com.ai.system.constants.SystemConstants.MenuConstants;
import com.ai.system.domain.MenuType;
import com.ai.system.domain.dto.MenuCreateDTO;
import com.ai.system.domain.dto.MenuDTO;
import com.ai.system.domain.dto.MenuPageDTO;
import com.ai.system.domain.entity.Menu;
import com.ai.system.domain.entity.RoleMenu;
import com.ai.system.domain.entity.UserRole;
import com.ai.system.domain.vo.MenuPageVO;
import com.ai.system.domain.vo.MenuVO;
import com.ai.system.repository.MenuRepository;
import com.ai.system.repository.RoleMenuRepository;
import com.ai.system.repository.UserRoleRepository;
import com.ai.system.security.AuthUserCacheService;
import com.ai.system.security.AuthorizationCacheService;
import com.ai.system.service.IMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MenuServiceImpl extends BaseServiceImpl<MenuPageDTO, MenuPageVO, MenuVO, MenuCreateDTO, MenuDTO, Menu> implements IMenuService {

    private static final Comparator<MenuVO> MENU_COMPARATOR = Comparator
            .comparingInt((MenuVO menu) -> menu.getSortOrder() == null ? Integer.MAX_VALUE : menu.getSortOrder())
            .thenComparing(menu -> menu.getCreateTime() == null ? java.time.LocalDateTime.MIN : menu.getCreateTime());

    private final MenuRepository repository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final AuthorizationCacheService authorizationCacheService;
    private final ObjectProvider<AuthUserCacheService> authUserCacheServiceProvider;

    @Override
    protected BaseRepository<MenuPageVO, MenuVO, Menu> getRepository() {
        return this.repository;
    }

    @Override
    protected void beforeCreate(Menu entity, MenuCreateDTO dto) {
        if (!StringUtils.hasText(entity.getId())) {
            entity.setId(UUID.randomUUID().toString());
        }
        validateParentId(null, dto.parentId());
    }

    @Override
    protected void beforeUpdate(Menu entity, MenuDTO dto) {
        validateParentId(entity.getId(), dto.parentId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(MenuCreateDTO dto) {
        return super.create(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, MenuDTO dto) {
        Set<String> affectedUserIds = findUserIdsByMenu(id);
        super.update(id, dto);
        evictAuthorizationUsers(affectedUserIds);
        refreshAuthUsers(affectedUserIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        Set<String> affectedUserIds = findUserIdsByMenu(id);
        super.delete(id);
        evictAuthorizationUsers(affectedUserIds);
        refreshAuthUsers(affectedUserIds);
    }

    @Override
    protected Specification<Menu> buildQuerySpec(MenuPageDTO pageDTO) {
        return SpecUtils.<Menu>builder()
                .andIf(true, () -> (root, query, cb) -> cb.conjunction())
                .eqIfPresent(Menu::getParentId, pageDTO != null ? pageDTO.getParentId() : null)
                .likeIfPresent(Menu::getMenuName, pageDTO != null ? pageDTO.getMenuName() : null)
                .likeIfPresent(Menu::getMenuTitle, pageDTO != null ? pageDTO.getMenuTitle() : null)
                .eqIfPresent(Menu::getMenuType, pageDTO != null ? pageDTO.getMenuType() : null)
                .eqIfPresent(Menu::getVisible, pageDTO != null ? pageDTO.getVisible() : null)
                .build();
    }

    @Override
    public List<MenuVO> tree() {
        return buildTree(loadMenuViews(repository.findByDeletedFalse()));
    }

    @Override
    public List<MenuVO> getAssignableMenus() {
        return loadMenuViews(repository.findByEnabledTrueAndDeletedFalse());
    }

    @Override
    public List<MenuVO> getAssignableMenuTree() {
        return buildTree(getAssignableMenus());
    }

    @Override
    public List<MenuVO> getUserMenus() {

        String userId = SecurityUtils.getUserId();

        if (userId == null || userId.isEmpty()) {
            return new ArrayList<>();
        }

        // 检查是否是超级管理员
        boolean isSuperAdmin = SecurityUtils.isSuperAdmin();

        if (isSuperAdmin) {
            // 超级管理员返回所有可见的导航菜单，不返回按钮权限节点。
            List<MenuVO> allMenus = getAssignableMenus();
            List<MenuVO> visibleMenus = allMenus.stream()
                    .filter(this::isVisibleNavigationMenu)
                    .collect(Collectors.toList());

            return buildTree(visibleMenus);
        }

        // 普通用户：根据角色权限查询菜单

        // 1. 查询用户的所有角色ID
        List<String> roleIds = userRoleRepository.findByUserId(userId)
                .stream()
                .map(com.ai.system.domain.entity.UserRole::getRoleId)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 根据角色ID查询所有菜单ID（去重）
        List<String> menuIds = roleMenuRepository.findMenuIdsByRoleIds(roleIds);

        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 查询菜单详情，并自动补齐已授权页面的可见父目录链。
        List<MenuVO> allMenus = getAssignableMenus();
        List<MenuVO> userMenus = includeAuthorizedNavigationMenus(allMenus, menuIds);

        return buildTree(userMenus);
    }

    /**
     * 返回角色已授权页面及其完整父目录链。
     *
     * <p>按钮节点只参与权限计算；不可见、停用、孤立或循环父链均按关闭策略处理。</p>
     */
    List<MenuVO> includeAuthorizedNavigationMenus(List<MenuVO> allMenus, Collection<String> authorizedIds) {
        if (allMenus == null || allMenus.isEmpty() || authorizedIds == null || authorizedIds.isEmpty()) {
            return List.of();
        }

        Map<String, MenuVO> menuById = allMenus.stream()
                .filter(Objects::nonNull)
                .filter(menu -> StringUtils.hasText(menu.getId()))
                .collect(Collectors.toMap(MenuVO::getId, menu -> menu, (first, ignored) -> first, LinkedHashMap::new));
        Set<String> includedIds = new LinkedHashSet<>();

        for (String authorizedId : authorizedIds) {
            String cursor = authorizedId;
            Set<String> visited = new HashSet<>();
            List<String> chain = new ArrayList<>();
            boolean validChain = true;
            while (StringUtils.hasText(cursor)) {
                if (!visited.add(cursor)) {
                    validChain = false;
                    break;
                }
                MenuVO menu = menuById.get(cursor);
                if (!isVisibleNavigationMenu(menu)) {
                    validChain = false;
                    break;
                }
                chain.add(menu.getId());
                cursor = normalizeParentId(menu.getParentId());
            }
            if (validChain) includedIds.addAll(chain);
        }

        return menuById.values().stream()
                .filter(menu -> includedIds.contains(menu.getId()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSort(List<String> ids) {

        if (ids == null || ids.isEmpty()) {
            return false;
        }

        try {
            // 根据传入的ID顺序更新菜单的sort字段
            for (int i = 0; i < ids.size(); i++) {
                String menuId = ids.get(i);
                Menu menu = repository.findById(menuId).orElse(null);

                if (menu != null) {
                    menu.setSortOrder(i);
                    repository.save(menu);
                }
            }

            return true;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Boolean checkPath(String path) {

        if (path == null || path.isEmpty()) {
            return false;
        }

        return !repository.existsByPathAndDeletedFalse(path);
    }

    @Override
    public List<MenuVO> getAvailableParents() {

        List<MenuVO> availableParents = getAssignableMenus().stream()
                .filter(menu -> menu.getVisible() != null && menu.getVisible())
                .filter(menu -> menu.getMenuType() != MenuType.BUTTON)
                .collect(Collectors.toList());

        return availableParents;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateVisibility(String id, Boolean visible) {

        if (id == null || id.isEmpty()) {
            return false;
        }

        if (visible == null) {
            return false;
        }

        try {
            Set<String> affectedUserIds = findUserIdsByMenu(id);
            Menu menu = repository.findById(id).orElse(null);

            if (menu == null) {
                return false;
            }

            // 更新可见性
            menu.setVisible(visible);
            repository.save(menu);
            evictAuthorizationUsers(affectedUserIds);
            refreshAuthUsers(affectedUserIds);

            return true;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 构建树形结构。先按 parentId 分组，避免每层递归都全量扫描菜单。
     */
    private List<MenuVO> buildTree(List<MenuVO> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, MenuVO> menuById = new LinkedHashMap<>();
        for (MenuVO menu : menus) {
            if (menu == null || !StringUtils.hasText(menu.getId())) {
                continue;
            }
            menu.setChildren(new ArrayList<>());
            menuById.put(menu.getId(), menu);
        }

        Map<String, List<MenuVO>> childrenByParent = new HashMap<>();
        List<MenuVO> roots = new ArrayList<>();
        Set<String> allIds = menuById.keySet();

        for (MenuVO menu : menuById.values()) {
            String menuId = menu.getId();
            String parentId = normalizeParentId(menu.getParentId());
            if (!StringUtils.hasText(parentId)) {
                roots.add(menu);
                continue;
            }
            if (menuId.equals(parentId)) {
                continue;
            }
            if (!allIds.contains(parentId)) {
                continue;
            }
            childrenByParent.computeIfAbsent(parentId, key -> new ArrayList<>()).add(menu);
        }

        childrenByParent.values().forEach(children -> children.sort(MENU_COMPARATOR));
        roots.sort(MENU_COMPARATOR);

        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (MenuVO root : roots) {
            attachChildren(root, childrenByParent, visited, visiting);
        }
        roots.sort(MENU_COMPARATOR);
        return roots;
    }

    private void attachChildren(
            MenuVO menu,
            Map<String, List<MenuVO>> childrenByParent,
            Set<String> visited,
            Set<String> visiting
    ) {
        String menuId = menu.getId();
        if (!StringUtils.hasText(menuId) || visited.contains(menuId)) {
            return;
        }
        if (!visiting.add(menuId)) {
            return;
        }

        List<MenuVO> children = childrenByParent.getOrDefault(menuId, List.of());
        List<MenuVO> safeChildren = new ArrayList<>(children.size());
        for (MenuVO child : children) {
            String childId = child.getId();
            if (!StringUtils.hasText(childId) || childId.equals(menuId)) {
                log.warn("菜单树检测到非法子节点，已跳过: parentId={}, childId={}", menuId, childId);
                continue;
            }
            if (visiting.contains(childId)) {
                log.warn("菜单树检测到循环引用，已跳过子节点: parentId={}, childId={}", menuId, childId);
                continue;
            }
            if (visited.contains(childId)) {
                log.warn("菜单树检测到重复挂载子节点，已跳过: parentId={}, childId={}", menuId, childId);
                continue;
            }
            attachChildren(child, childrenByParent, visited, visiting);
            safeChildren.add(child);
        }

        menu.setChildren(safeChildren);
        visiting.remove(menuId);
        visited.add(menuId);
    }

    private void validateParentId(String currentId, String parentId) {
        String normalizedParentId = normalizeParentId(parentId);
        if (!StringUtils.hasText(normalizedParentId)) {
            return;
        }
        if (StringUtils.hasText(currentId) && normalizedParentId.equals(currentId)) {
            throw new BizException("上级菜单不能是自己");
        }

        List<Menu> menus = repository.findByDeletedFalse();
        Map<String, String> parentIdById = new LinkedHashMap<>();
        for (Menu menu : menus) {
            if (StringUtils.hasText(menu.getId())) {
                parentIdById.put(menu.getId(), normalizeParentId(menu.getParentId()));
            }
        }

        if (!parentIdById.containsKey(normalizedParentId)) {
            throw new BizException("上级菜单不存在");
        }
        if (!StringUtils.hasText(currentId)) {
            ensureParentChainNotCyclic(normalizedParentId, parentIdById);
            return;
        }

        String cursor = normalizedParentId;
        Set<String> visited = new HashSet<>();
        while (StringUtils.hasText(cursor)) {
            if (currentId.equals(cursor)) {
                throw new BizException("上级菜单不能选择当前菜单或其子菜单");
            }
            if (!visited.add(cursor)) {
                throw new BizException("菜单父级存在循环引用，请先修复菜单层级");
            }
            cursor = parentIdById.get(cursor);
        }
    }

    private void ensureParentChainNotCyclic(String parentId, Map<String, String> parentIdById) {
        String cursor = parentId;
        Set<String> visited = new HashSet<>();
        while (StringUtils.hasText(cursor)) {
            if (!visited.add(cursor)) {
                throw new BizException("菜单父级存在循环引用，请先修复菜单层级");
            }
            cursor = parentIdById.get(cursor);
        }
    }

    private String normalizeParentId(String parentId) {
        if (!StringUtils.hasText(parentId)) {
            return null;
        }
        String trimmedParentId = parentId.trim();
        return MenuConstants.ROOT_PARENT_ID.equals(trimmedParentId) ? null : trimmedParentId;
    }

    private boolean isVisibleNavigationMenu(MenuVO menu) {
        if (menu == null || !Boolean.TRUE.equals(menu.getEnabled()) || !Boolean.TRUE.equals(menu.getVisible())) {
            return false;
        }
        return menu.getMenuType() != null && menu.getMenuType().isNavigation();
    }

    private Set<String> toPermissionSet(Collection<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        return permissions.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getUserPermissions(String userId, Boolean superAdmin) {
        if (Boolean.TRUE.equals(superAdmin)) {
            return toPermissionSet(repository.findAllPermissions());
        }

        // 1. 获取用户的所有角色ID
        List<String> roleIds = userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRoleId)
                .toList();

        if (roleIds.isEmpty()) {
            return Set.of();
        }

        // 2. 获取角色关联的所有菜单ID
        List<String> menuIds = roleMenuRepository.findMenuIdsByRoleIds(roleIds);

        if (menuIds.isEmpty()) {
            return Set.of();
        }

        return toPermissionSet(repository.findPermissionsByIds(menuIds));
    }

    @Override
    public PageResponse<MenuPageVO> list(MenuPageDTO pageDTO) {
        PageResponse<MenuPageVO> pageResponse = super.list(pageDTO);
        fillPageTypeLabel(pageResponse);
        return pageResponse;
    }

    private List<MenuVO> loadMenuViews(List<Menu> menus) {
        List<MenuVO> menuViews = EntityConverter.toVoList(menus, MenuVO.class);
        fillTypeLabel(menuViews);
        return menuViews;
    }

    @Override
    public MenuVO detail(String id) {
        MenuVO menu = super.detail(id);
        fillTypeLabel(menu);
        return menu;
    }

    private void fillTypeLabel(List<MenuVO> menus) {
        if (menus == null || menus.isEmpty()) {
            return;
        }
        menus.forEach(this::fillTypeLabel);
    }

    private void fillTypeLabel(MenuVO menu) {
        if (menu == null || menu.getMenuType() == null || StringUtils.hasText(menu.getTypeLabel())) {
            return;
        }
        menu.setTypeLabel(menu.getMenuType().label());
    }

    private void fillPageTypeLabel(PageResponse<MenuPageVO> pageResponse) {
        if (pageResponse == null || pageResponse.isEmpty()) {
            return;
        }
        for (MenuPageVO menu : pageResponse.records()) {
            if (menu == null || menu.getMenuType() == null || StringUtils.hasText(menu.getTypeLabel())) {
                continue;
            }
            menu.setTypeLabel(menu.getMenuType().label());
        }
    }

    private Set<String> findUserIdsByMenu(String menuId) {
        return roleMenuRepository.findByMenuId(menuId).stream()
                .map(RoleMenu::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(roleId -> userRoleRepository.findByRoleId(roleId).stream())
                .map(UserRole::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void refreshAuthUsers(Set<String> userIds) {
        AuthUserCacheService authUserCacheService = authUserCacheServiceProvider.getIfAvailable();
        if (authUserCacheService != null) {
            authUserCacheService.refreshUsersAfterCommit(userIds, true);
        }
    }

    private void evictAuthorizationUsers(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        userIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .forEach(authorizationCacheService::evictUser);
    }

}
