package com.ai.system.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataEntity;
import io.github.guanxiangkai.web.plus.core.entity.SortInfo;
import io.github.guanxiangkai.web.plus.core.entity.Sortable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 行政区域实体
 * <p>
 * 树形层级：省 → 市 → 区/县 → 街道/乡镇
 * 编码遵循国标 GB/T 2260 行政区划代码
 * </p>
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_region", comment = "行政区域表")
public class Region extends DataEntity implements Sortable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder.Default
    @Embedded
    private SortInfo sortInfo = new SortInfo();

    /**
     * 城市编码（行政区划编码，GB/T 2260）
     */
    @Column(name = "region_code", nullable = false, length = 20, comment = "区域编码")
    private String regionCode;

    /**
     * 区域名称
     */
    @Column(name = "region_name", nullable = false, length = 100, comment = "区域名称")
    private String regionName;

    /**
     * 父区域ID（根节点为空）
     */
    @Column(name = "parent_id", length = 64, comment = "父区域ID")
    private String parentId;

    /**
     * 类型（province省级、city市级、district区级、street街道级）
     */
    @Column(name = "region_level", nullable = false, length = 64, comment = "层级")
    private String regionLevel;

    /**
     * 完整路径名称（例："北京市/朝阳区/三里屯街道"）
     */
    @Column(name = "full_name", length = 500, comment = "完整路径名称")
    private String fullName;

    /**
     * 区域简称
     */
    @Column(name = "short_name", length = 50, comment = "区域简称")
    private String shortName;

    /**
     * 经度
     */
    @Column(name = "longitude", precision = 10, scale = 6, comment = "经度")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @Column(name = "latitude", precision = 10, scale = 6, comment = "纬度")
    private BigDecimal latitude;

    /**
     * 邮政编码
     */
    @Column(name = "zip_code", length = 10, comment = "邮政编码")
    private String zipCode;

}
