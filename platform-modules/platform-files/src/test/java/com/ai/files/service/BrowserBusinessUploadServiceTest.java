package com.ai.files.service;

import com.ai.api.files.dto.*;
import com.ai.files.config.*;
import com.ai.files.domain.*;
import com.ai.files.domain.entity.*;
import com.ai.files.repository.*;
import io.github.guanxiangkai.web.plus.security.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BrowserBusinessUploadServiceTest {
    private static final String HASH="a".repeat(64);
    private final FilesService files=mock(FilesService.class);
    private final FileAccessService access=mock(FileAccessService.class);
    private final FileSpaceRepository spaces=mock(FileSpaceRepository.class);
    private final FileNodeRepository nodes=mock(FileNodeRepository.class);
    private final FileVersionRepository versions=mock(FileVersionRepository.class);
    private final FileUploadRecordRepository uploads=mock(FileUploadRecordRepository.class);
    private final FileBrowserUploadTargetRepository targets=mock(FileBrowserUploadTargetRepository.class);
    private final FileGrantRepository grants=mock(FileGrantRepository.class);
    private final TransactionTemplate tx=mock(TransactionTemplate.class);
    private BrowserBusinessUploadService service;
    private MockedStatic<SecurityUtils> security;
    private FileBrowserUploadTarget target;
    private FileNode file;
    private FileVersion version;
    private FileUploadRecord upload;
    private FileGrant grant;

    @BeforeEach void setup() {
        security=mockStatic(SecurityUtils.class);security.when(SecurityUtils::getTenantId).thenReturn("tenant");
        when(access.isInternalService()).thenReturn(true);
        when(tx.execute(any())).thenAnswer(call->((TransactionCallback<?>)call.getArgument(0)).doInTransaction(null));
        service=new BrowserBusinessUploadService(files,access,spaces,nodes,versions,uploads,targets,grants,
                new FilesProperties(null,null,null,null,null,null,null,null,null,null,null),new BrowserUploadProperties(null),tx);
        var space=new FileSpace();space.setId("space");space.setTenantId("tenant");space.setSpaceType(FileSpaceType.SYSTEM);
        when(files.systemSpace()).thenReturn(space);when(spaces.findLockedByIdAndTenantId("space","tenant")).thenReturn(Optional.of(space));
        var parent=node("parent","/upload",FileNodeType.FOLDER);var accepted=node("accepted","/accepted",FileNodeType.FOLDER);
        parent.setParentId("container");
        var container=node("container","/upload",FileNodeType.FOLDER);container.setNodeName("type-"+FileChecksum.sha256("BROWSER_UPLOAD").substring(0,24));
        when(nodes.findLockedByIdAndTenantId("container","tenant")).thenReturn(Optional.of(container));
        when(files.systemBusinessFolder(eq(space),eq("BROWSER_UPLOAD"),anyString())).thenReturn(parent);
        when(files.systemBusinessFolder(eq(space),eq("BROWSER_ACCEPTED"),anyString())).thenReturn(accepted);
        when(nodes.findLockedByIdAndTenantId("parent","tenant")).thenReturn(Optional.of(parent));
        target=new FileBrowserUploadTarget();target.setId("parent");target.setTenantId("tenant");target.setSpaceId("space");target.setStagingParentId("container");target.setBusinessType("PROJECT");target.setBusinessId("reservation");target.setUploaderUserId("owner");target.setFilename("a.txt");target.setSizeBytes(3);target.setSha256(HASH);target.setExpiresAt(Instant.now().plusSeconds(60));target.setStatus("UPLOADED");target.setFileId("file");target.setVersionId("version");
        file=node("file","/upload/a.txt",FileNodeType.FILE);file.setParentId("parent");file.setNodeName("a.txt");file.setCurrentVersionId("version");file.setBusinessType("PROJECT");file.setBusinessId("reservation");
        when(nodes.findLockedByIdAndTenantId("file","tenant")).thenReturn(Optional.of(file));
        version=new FileVersion();version.setId("version");version.setNodeId("file");version.setTenantId("tenant");version.setOriginalName("a.txt");version.setSizeBytes(3L);version.setSha256(HASH);version.setVersionState(FileVersionState.AVAILABLE);version.setObjectKey("private-object-key");
        when(versions.findByIdAndNodeIdAndTenantIdAndVersionStateAndDeletedFalse("version","file","tenant",FileVersionState.AVAILABLE)).thenReturn(Optional.of(version));
        upload=new FileUploadRecord();upload.setUploadState(FileUploadState.COMPLETED);upload.setOperatorUserId("owner");upload.setBusinessType("PROJECT");upload.setBusinessId("reservation");
        when(uploads.findLockedVersion("tenant","file","version")).thenReturn(Optional.of(upload));
        when(targets.findLockedByIdAndTenantId("parent","tenant")).thenReturn(Optional.of(target));
        grant=new FileGrant();grant.setDeleted(false);when(grants.findBySpaceIdAndNodeIdAndPrincipalTypeAndPrincipalIdAndDeletedFalse("space","parent",FilePrincipalType.USER,"owner")).thenReturn(Optional.of(grant));
    }
    @AfterEach void close(){security.close();}

    @Test void policyRequiresInternalIdentityAndRejectsInvalidConcurrency() {
        assertThat(service.policy().concurrency()).isEqualTo(3);
        assertThatThrownBy(()->new BrowserUploadProperties(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new BrowserUploadProperties(9)).isInstanceOf(IllegalArgumentException.class);
        when(access.isInternalService()).thenReturn(false);
        assertThatThrownBy(service::policy).hasMessageContaining("可信内部");
    }
    @Test void preparePersistsExactManifestAndOnlyGrantsTheReservedFolder() {
        var result=service.prepare(new FileBrowserUploadTargetRequestDTO("PROJECT","reservation","owner","a.txt",3,HASH));
        assertThat(result.parentId()).isEqualTo("parent");assertThat(result.uploadedFile()).isNull();
        verify(targets).saveAndFlush(argThat(value->value.getUploaderUserId().equals("owner")&&value.getSha256().equals(HASH)&&value.getStatus().equals("OPEN")));
        assertThat(grant.getGrantRole()).isEqualTo(FileRole.UPLOADER);assertThat(grant.getInherited()).isTrue();assertThat(grant.getExpiresAt()).isAfter(Instant.now());
    }
    @Test void prepareCannotChangeOwnerOrManifestAndRecoversASealedVersion() {
        when(targets.findLockedBusiness("tenant","PROJECT","reservation")).thenReturn(Optional.of(target));
        assertThatThrownBy(()->service.prepare(new FileBrowserUploadTargetRequestDTO("PROJECT","reservation","other","a.txt",3,HASH))).hasMessageContaining("不匹配");
        assertThatThrownBy(()->service.prepare(new FileBrowserUploadTargetRequestDTO("PROJECT","reservation","owner","a.txt",4,HASH))).hasMessageContaining("不匹配");
        target.setStatus("ACCEPTED");target.setAcceptedParentId("accepted");file.setParentId("accepted");
        var result=service.prepare(new FileBrowserUploadTargetRequestDTO("PROJECT","reservation","owner","a.txt",3,HASH));
        assertThat(result.uploadedFile().versionId()).isEqualTo("version");assertThat(result.uploadedFile().storeName()).isNull();
        verify(grants,never()).saveAndFlush(any());
    }
    @Test void acceptSealsAtomicallyAndCanRepeatAfterResponseLoss() {
        var first=service.accept(request());assertThat(first.parentId()).isEqualTo("accepted");assertThat(first.file().hash()).isEqualTo(HASH);assertThat(first.file().storeName()).isNull();
        assertThat(target.getStatus()).isEqualTo("ACCEPTED");assertThat(grant.getDeleted()).isTrue();
        var second=service.accept(request());assertThat(second.file().versionId()).isEqualTo(first.file().versionId());
        verify(nodes,times(1)).saveAndFlush(file);
    }
    @Test void acceptRejectsActualUploaderMismatchBeforeMovingAnything() {
        upload.setOperatorUserId("other");assertThatThrownBy(()->service.accept(request())).hasMessageContaining("不匹配");verify(nodes,never()).saveAndFlush(any());
    }
    @Test void acceptRejectsWrongParentChangedVersionAndActiveUpload() {
        file.setParentId("other-parent");assertThatThrownBy(()->service.accept(request())).hasMessageContaining("不匹配");
        file.setParentId("parent");file.setCurrentVersionId("changed");assertThatThrownBy(()->service.accept(request())).hasMessageContaining("不匹配");
        file.setCurrentVersionId("version");file.setActiveUploadId("pending");assertThatThrownBy(()->service.accept(request())).hasMessageContaining("不匹配");verify(nodes,never()).saveAndFlush(any());
    }
    @Test void acceptRejectsAuthoritativeContentMismatchAndForeignTenant() {
        version.setSha256("b".repeat(64));assertThatThrownBy(()->service.accept(request())).hasMessageContaining("不匹配");
        version.setSha256(HASH);file.setTenantId("other");assertThatThrownBy(()->service.accept(request())).hasMessageContaining("不匹配");verify(nodes,never()).saveAndFlush(any());
    }
    private FileBrowserUploadAcceptRequestDTO request(){return new FileBrowserUploadAcceptRequestDTO("PROJECT","reservation","owner","parent","file","version","a.txt",3,HASH);}
    private FileNode node(String id,String path,FileNodeType type){var node=new FileNode();node.setId(id);node.setTenantId("tenant");node.setSpaceId("space");node.setDisplayPath(path);node.setNodeType(type);node.setNodeState(FileNodeState.ACTIVE);return node;}
}
