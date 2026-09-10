package com.ai.files.service;

import com.ai.api.files.dto.*;
import com.ai.files.config.BrowserUploadProperties;
import com.ai.files.config.FilesProperties;
import com.ai.files.domain.*;
import com.ai.files.domain.entity.*;
import com.ai.files.repository.*;
import io.github.guanxiangkai.web.plus.error.exception.BizException;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 复用文件目录授权与上传 saga，约束浏览器只提交预约的一份原件。 */
@Service @RequiredArgsConstructor
public class BrowserBusinessUploadService {
    private final FilesService files;
    private final FileAccessService access;
    private final FileSpaceRepository spaces;
    private final FileNodeRepository nodes;
    private final FileVersionRepository versions;
    private final FileUploadRecordRepository uploads;
    private final FileBrowserUploadTargetRepository targets;
    private final FileGrantRepository grants;
    private final FilesProperties properties;
    private final BrowserUploadProperties browserProperties;
    private final TransactionTemplate transactions;

    /** 仅可信业务服务读取对浏览器公开的非秘密资源策略。 */
    public FileBrowserUploadPolicyDTO policy() {
        tenant();
        return new FileBrowserUploadPolicyDTO(browserProperties.concurrency(), properties.resolvedMaxFileSizeBytes());
    }

    /** 同一业务预约只能属于一个用户和一份确定原件，重试仅续期未封存目标。 */
    public FileBrowserUploadTargetDTO prepare(FileBrowserUploadTargetRequestDTO request) {
        String tenant = tenant();
        if (request == null) throw invalid();
        validate(request.businessType(), request.businessId(), request.uploaderUserId(), request.filename(), request.sizeBytes(), request.sha256());
        FileSpace system = files.systemSpace();
        return Objects.requireNonNull(transactions.execute(status -> {
            FileSpace space = spaces.findLockedByIdAndTenantId(system.getId(), tenant).orElseThrow(this::invalid);
            FileBrowserUploadTarget target = targets.findLockedBusiness(tenant,request.businessType(),request.businessId()).orElse(null);
            FileNode parent;
            if (target == null) {
                parent = files.systemBusinessFolder(space,"BROWSER_UPLOAD",scope(request.businessType(),request.businessId()));
                target = new FileBrowserUploadTarget();target.setId(parent.getId());target.setTenantId(tenant);
                target.setSpaceId(space.getId());target.setStagingParentId(parent.getParentId());target.setBusinessType(request.businessType());target.setBusinessId(request.businessId());
                target.setUploaderUserId(request.uploaderUserId());target.setFilename(request.filename());
                target.setSizeBytes(request.sizeBytes());target.setSha256(request.sha256());target.setStatus("OPEN");
            } else {
                match(target,request.businessType(),request.businessId(),request.uploaderUserId(),request.filename(),request.sizeBytes(),request.sha256());
                parent = nodes.findLockedByIdAndTenantId(target.getId(),tenant).orElseThrow(this::invalid);
            }
            if (parent.getNodeState()!=FileNodeState.ACTIVE || parent.getNodeType()!=FileNodeType.FOLDER
                    || !space.getId().equals(parent.getSpaceId()) || !Objects.equals(target.getStagingParentId(),parent.getParentId())) throw invalid();
            verifyStaging(target,parent,tenant);
            if (!"ACCEPTED".equals(target.getStatus())) {
                target.setExpiresAt(Instant.now().plus(properties.resolvedUploadLease()));
                grant(space,parent,request.uploaderUserId(),target.getExpiresAt());
                targets.saveAndFlush(target);
            }
            FileUploadResultDTO uploaded = target.getFileId()==null ? null : result(checkedNode(target,tenant),target.getVersionId(),tenant);
            return new FileBrowserUploadTargetDTO(space.getId(),parent.getId(),target.getExpiresAt(),uploaded);
        }));
    }

    /** 校验实际上传主体和版本，在同一事务内移入私有目录并撤回临时授权。 */
    public FileBusinessUploadDTO accept(FileBrowserUploadAcceptRequestDTO request) {
        String tenant = tenant();
        if (request==null) throw invalid();
        validate(request.businessType(),request.businessId(),request.uploaderUserId(),request.filename(),request.sizeBytes(),request.sha256());
        FileSpace system=files.systemSpace();
        return Objects.requireNonNull(transactions.execute(status -> {
            FileSpace space=spaces.findLockedByIdAndTenantId(system.getId(),tenant).orElseThrow(this::invalid);
            FileBrowserUploadTarget target=targets.findLockedByIdAndTenantId(text(request.uploadParentId(),64),tenant).orElseThrow(this::invalid);
            match(target,request.businessType(),request.businessId(),request.uploaderUserId(),request.filename(),request.sizeBytes(),request.sha256());
            if (!space.getId().equals(target.getSpaceId()) || !Objects.equals(target.getFileId(),request.fileId())
                    || !Objects.equals(target.getVersionId(),request.versionId())
                    || !("UPLOADED".equals(target.getStatus()) || "ACCEPTED".equals(target.getStatus()))) throw invalid();
            FileNode staging=nodes.findLockedByIdAndTenantId(target.getId(),tenant).orElseThrow(this::invalid);
            if (staging.getNodeState()!=FileNodeState.ACTIVE || staging.getNodeType()!=FileNodeType.FOLDER
                    || !space.getId().equals(staging.getSpaceId()) || !Objects.equals(target.getStagingParentId(),staging.getParentId())) throw invalid();
            verifyStaging(target,staging,tenant);
            FileNode node=checkedNode(target,tenant);
            FileVersion version=versions.findByIdAndNodeIdAndTenantIdAndVersionStateAndDeletedFalse(
                    request.versionId(),node.getId(),tenant,FileVersionState.AVAILABLE).orElseThrow(this::invalid);
            // COMPLETED 上传不再变化；活动上传在 checkedNode 中先被拒绝，避免与上传完成锁竞争。
            FileUploadRecord upload=uploads.findLockedVersion(tenant,node.getId(),version.getId()).orElseThrow(this::invalid);
            if (upload.getUploadState()!=FileUploadState.COMPLETED
                    || !request.uploaderUserId().equals(upload.getOperatorUserId())
                    || !request.businessType().equals(upload.getBusinessType()) || !request.businessId().equals(upload.getBusinessId())
                    || !request.businessType().equals(node.getBusinessType()) || !request.businessId().equals(node.getBusinessId())
                    || !request.filename().equals(node.getNodeName()) || !request.filename().equals(version.getOriginalName())
                    || !request.sha256().equals(version.getSha256()) || version.getSizeBytes()==null
                    || request.sizeBytes()!=version.getSizeBytes()) throw invalid();
            String expectedParent="ACCEPTED".equals(target.getStatus())?target.getAcceptedParentId():target.getId();
            if (!Objects.equals(node.getParentId(),expectedParent)) throw invalid();
            if (!"ACCEPTED".equals(target.getStatus())) {
                FileNode accepted=files.systemBusinessFolder(space,"BROWSER_ACCEPTED",scope(request.businessType(),request.businessId()));
                node.setParentId(accepted.getId());node.setDisplayPath(accepted.getDisplayPath()+"/"+node.getNodeName());
                nodes.saveAndFlush(node);
                target.setAcceptedParentId(accepted.getId());target.setStatus("ACCEPTED");targets.saveAndFlush(target);
            }
            grants.findBySpaceIdAndNodeIdAndPrincipalTypeAndPrincipalIdAndDeletedFalse(space.getId(),target.getId(),FilePrincipalType.USER,request.uploaderUserId())
                    .ifPresent(grant -> {grant.setExpiresAt(Instant.now());grant.setDeleted(true);grants.save(grant);});
            return new FileBusinessUploadDTO(result(node,version.getId(),tenant),null,node.getParentId(),null,node.getDisplayPath());
        }));
    }

    private void verifyStaging(FileBrowserUploadTarget target,FileNode staging,String tenant) {
        if (!Objects.equals(target.getStagingParentId(),staging.getParentId()) || !tenant.equals(staging.getTenantId())) throw invalid();
        FileNode container=nodes.findLockedByIdAndTenantId(target.getStagingParentId(),tenant).orElseThrow(this::invalid);
        if (container.getNodeState()!=FileNodeState.ACTIVE || container.getNodeType()!=FileNodeType.FOLDER
                || !target.getSpaceId().equals(container.getSpaceId()) || container.getParentId()!=null
                || !("type-"+FileChecksum.sha256("BROWSER_UPLOAD").substring(0,24)).equals(container.getNodeName())
                || files.businessRootForNode(staging)!=null) throw invalid();
    }

    private FileNode checkedNode(FileBrowserUploadTarget target,String tenant) {
        FileNode node=nodes.findLockedByIdAndTenantId(target.getFileId(),tenant).orElseThrow(this::invalid);
        if (!tenant.equals(node.getTenantId()) || !target.getSpaceId().equals(node.getSpaceId())
                || node.getNodeType()!=FileNodeType.FILE || node.getNodeState()!=FileNodeState.ACTIVE
                || node.getActiveUploadId()!=null || !Objects.equals(target.getVersionId(),node.getCurrentVersionId())) throw invalid();
        return node;
    }
    private FileUploadResultDTO result(FileNode node,String versionId,String tenant) {
        FileVersion version=versions.findByIdAndNodeIdAndTenantIdAndVersionStateAndDeletedFalse(versionId,node.getId(),tenant,FileVersionState.AVAILABLE).orElseThrow(this::invalid);
        return new FileUploadResultDTO(node.getId(),version.getId(),version.getOriginalName(),null,version.getContentType(),version.getSizeBytes(),"/files/"+node.getId(),version.getSha256());
    }
    private void grant(FileSpace space,FileNode parent,String user,Instant until) {
        FileGrant grant=grants.findBySpaceIdAndNodeIdAndPrincipalTypeAndPrincipalIdAndDeletedFalse(space.getId(),parent.getId(),FilePrincipalType.USER,user).orElseGet(() -> {
            FileGrant created=new FileGrant();created.setId(UUID.randomUUID().toString());created.setTenantId(space.getTenantId());
            created.setSpaceId(space.getId());created.setNodeId(parent.getId());created.setPrincipalType(FilePrincipalType.USER);created.setPrincipalId(user);
            return created;
        });
        grant.setGrantRole(FileRole.UPLOADER);grant.setInherited(true);grant.setExpiresAt(until);grant.setStatus(FileRecordStatus.ACTIVE.name());grants.saveAndFlush(grant);
    }
    private void match(FileBrowserUploadTarget target,String type,String business,String user,String filename,long size,String hash) {
        if (!target.getBusinessType().equals(type)||!target.getBusinessId().equals(business)||!target.getUploaderUserId().equals(user)
                ||!target.getFilename().equals(filename)||target.getSizeBytes()!=size||!target.getSha256().equals(hash)) throw invalid();
    }
    private void validate(String type,String business,String user,String filename,long size,String hash) {
        text(type,64);text(business,128);text(user,64);text(filename,256);
        if (filename.contains("/")||filename.contains("\\")||filename.equals(".")||filename.equals("..")
                || filename.chars().anyMatch(Character::isISOControl)||size<1||size>properties.resolvedMaxFileSizeBytes()
                ||hash==null||!hash.matches("[0-9a-f]{64}")
                ||io.github.guanxiangkai.web.plus.core.constants.AuthConstants.HeaderConstants.INTERNAL_SERVICE_USER_ID.equals(user)) throw invalid();
    }
    private String text(String value,int max) {
        if(value==null||value.isBlank()||!value.equals(value.trim())||value.length()>max)throw invalid();return value;
    }
    private String tenant() {
        if (!access.isInternalService()) throw new BizException("仅可信内部服务可以管理业务上传预约");
        return text(SecurityUtils.getTenantId(),64);
    }
    private String scope(String type,String id){return FileChecksum.sha256(type.length()+":"+type+id);}
    private BizException invalid(){return new BizException("上传预约、原件版本或授权范围不匹配");}
}
