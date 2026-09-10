package com.ai.system.domain.vo;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TreeViewJsonContractTest {

    @Test
    void inheritedDepartmentFieldsKeepTheFlatJsonContract() {
        DeptVO dept = new DeptVO();
        dept.setId("department-1");
        dept.setDeptName("研发组");
        dept.setRemark("部门备注");
        dept.setEnabled(true);
        dept.setSortOrder(3);
        dept.setChildren(List.of());
        Map<String, Object> json = JsonMapper.builder().build().convertValue(dept, new TypeReference<>() {});

        assertThat(json.keySet()).containsExactlyInAnyOrder("id", "createTime", "updateTime", "remark",
                "enabled", "sortOrder", "deptName", "parentId", "deptCode", "location", "regionCode",
                "leaderId", "leaderName", "phone", "email", "ancestors", "children");
        assertThat(json).containsEntry("id", "department-1").containsEntry("remark", "部门备注")
                .containsEntry("enabled", true).containsEntry("sortOrder", 3).containsEntry("children", List.of());
    }

    @Test
    void regionCompositionDoesNotIntroduceAdditionalAuditFields() {
        RegionVO region = new RegionVO();
        region.setId("region-1");
        region.setChildren(List.of());
        Map<String, Object> json = JsonMapper.builder().build().convertValue(region, new TypeReference<>() {});

        assertThat(json.keySet()).containsExactlyInAnyOrder("id", "regionCode", "regionName", "parentId",
                "regionLevel", "fullName", "shortName", "longitude", "latitude", "zipCode", "enabled",
                "sortOrder", "remark", "createTime", "children");
    }
}
