package com.ai.files.domain.entity;

import io.github.guanxiangkai.web.plus.core.entity.DataTenantEntity;
import com.ai.files.domain.FileNodeState;
import com.ai.files.domain.FileNodeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.time.Instant;

/**
 * 文件树中的文件或文件夹节点。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "file_node", comment = "文件节点表")
public class FileNode extends DataTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属空间标识。 */
    @Column(name = "space_id", nullable = false, length = 64, comment = "文件空间标识")
    private String spaceId;

    /** 父文件夹标识；根节点为空。 */
    @Column(name = "parent_id", length = 64, comment = "父文件夹标识")
    private String parentId;

    /** 节点类型。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 16, comment = "文件节点类型")
    private FileNodeType nodeType;

    /** 文件或文件夹名称。 */
    @Column(name = "node_name", nullable = false, length = 256, comment = "文件或文件夹名称")
    private String nodeName;

    /** 仅用于展示和树查询的逻辑路径。 */
    @Column(name = "display_path", nullable = false, length = 2048, comment = "展示与树查询逻辑路径")
    private String displayPath;

    /** 当前文件版本标识；文件夹为空。 */
    @Column(name = "current_version_id", length = 64, comment = "当前文件版本标识")
    private String currentVersionId;

    /** 当前占用节点版本边界的上传记录标识。 */
    @Column(name = "active_upload_id", length = 64, comment = "当前活动上传记录标识")
    private String activeUploadId;

    /** 节点生命周期状态。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "node_state", nullable = false, length = 16, comment = "文件节点状态")
    private FileNodeState nodeState = FileNodeState.ACTIVE;

    /** 首次进入回收站的时间。 */
    @Column(name = "trashed_at", comment = "移入回收站时间")
    private Instant trashedAt;

    /** 执行回收操作的用户标识。 */
    @Column(name = "trashed_by", length = 64, comment = "移入回收站用户标识")
    private String trashedBy;

    /** 来源业务类型。 */
    @Column(name = "business_type", length = 64, comment = "来源业务类型")
    private String businessType;

    /** 来源业务标识。 */
    @Column(name = "business_id", length = 128, comment = "来源业务标识")
    private String businessId;
}
