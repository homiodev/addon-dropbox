package org.homio.addon.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeleteResult;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.MediaInfo;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.SharingInfo;
import com.dropbox.core.v2.users.SpaceUsage;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Bytes;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.homio.api.fs.FileSystemProvider;
import org.homio.api.fs.TreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DropboxFileSystem implements FileSystemProvider {

  private static final DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox").build();
  private final LoadingCache<String, Metadata> fileCache;
  private DropboxEntity entity;
  private DbxClientV2 drive;
  private SpaceUsage spaceUsage;
  private long aboutSpaceUsage;
  private long connectionHashCode;

  public DropboxFileSystem(DropboxEntity entity) {
    this.entity = entity;
    this.fileCache = CacheBuilder.newBuilder().
        expireAfterWrite(1, TimeUnit.HOURS).build(new CacheLoader<>() {
          public Metadata load(@NotNull String id) throws DbxException {
            return getDrive().files().getMetadata(id);
          }
        });
  }

  @SneakyThrows
  public SpaceUsage getAbout() {
    if (spaceUsage == null || System.currentTimeMillis() - aboutSpaceUsage > 600000) {
      spaceUsage = getDrive().users().getSpaceUsage();
      aboutSpaceUsage = System.currentTimeMillis();
    }
    return spaceUsage;
  }

  public void dispose() {
    this.drive = null;
    // this.setRoot(new DropboxCacheFileSystem(new DropboxFile(new Metadata("")), null));
  }

  @Override
  public boolean restart(boolean force) {
    try {
      if (!force && connectionHashCode == entity.getConnectionHashCode()) {
        return true;
      }
      dispose();
      getChildren("");
      entity.setStatusOnline();
      connectionHashCode = entity.getConnectionHashCode();
      return true;
    } catch (Exception ex) {
      entity.setStatusError(ex);
      return false;
    }
  }

  @Override
  public void setEntity(Object entity) {
    this.entity = (DropboxEntity) entity;
    restart(false);
  }

  @Override
  @SneakyThrows
  public boolean exists(@NotNull String id) {
    return fileCache.get(id) != null;
  }

  @Override
  @SneakyThrows
  public @NotNull InputStream getEntryInputStream(@NotNull String id) {
    return getDrive().files().download(id).getInputStream();
  }

  @Override
  @SneakyThrows
  public Set<TreeNode> toTreeNodes(@NotNull Set<String> ids) {
    Set<TreeNode> fmPaths = new HashSet<>();
    for (String id : ids) {
      Metadata file = fileCache.get(id);
      if (file != null) {
        fmPaths.add(buildTreeNode(file));
      }
    }
    return fmPaths;
  }

  @Override
  public long getTotalSpace() {
    return getAbout().getAllocation().getIndividualValue().getAllocated();
  }

  @Override
  public long getUsedSpace() {
    return getAbout().getUsed();
  }

  @Override
  @SneakyThrows
  public TreeNode delete(@NotNull Set<String> ids) {
    List<Metadata> files = new ArrayList<>();
    for (String id : ids) {
      fileCache.invalidate(id);
      DeleteResult deleteResult = getDrive().files().deleteV2(id);
      if (deleteResult != null && deleteResult.getMetadata() != null) {
        files.add(deleteResult.getMetadata());
      }
    }
    return buildRoot(files);
  }

  @Override
  @SneakyThrows
  public TreeNode create(@NotNull String parentId, @NotNull String name, boolean isDir, UploadOption uploadOption) {
    String path = Paths.get(parentId).resolveSibling(name).toString();
    Metadata file;
    if (uploadOption != UploadOption.Replace) {
      Metadata existedFile = getDrive().files().getMetadata(path);
      if (existedFile != null) {
        if (uploadOption == UploadOption.SkipExist) {
          return null;
        } else if (uploadOption == UploadOption.Error) {
          throw new FileAlreadyExistsException("File " + name + " already exists");
        }
      }
    }
    if (isDir) {
      file = getDrive().files().createFolderV2(path).getMetadata();
    } else {
      file = getDrive().files().uploadBuilder(path).uploadAndFinish(new ByteArrayInputStream(new byte[0]));
    }
    return buildRoot(Collections.singleton(file));
  }

  @Override
  @SneakyThrows
  public TreeNode rename(@NotNull String id, @NotNull String newName, UploadOption uploadOption) {
    Metadata file = fileCache.get(id);
    if (file != null) {
      String toPath = Paths.get(id).resolveSibling(newName).toString();

      if (uploadOption != UploadOption.Replace) {
        Metadata existedFile = getDrive().files().getMetadata(toPath);
        if (existedFile != null) {
          if (uploadOption == UploadOption.SkipExist) {
            return null;
          } else if (uploadOption == UploadOption.Error) {
            throw new FileAlreadyExistsException("File " + newName + " already exists");
          }
        }
      }

      getDrive().files().moveV2(id, toPath);
      return buildRoot(Collections.singleton(file));
    }
    throw new IllegalStateException("File '" + id + "' not found");
  }

  @Override
  @SneakyThrows
  public Set<TreeNode> getChildrenRecursively(@NotNull String parentId) {
    TreeNode root = new TreeNode();
    buildTreeNodeRecursively(parentId, root);
    return root.getChildren();
  }

  private void buildTreeNodeRecursively(String parentId, TreeNode parent)
      throws DbxException {
    ListFolderResult result = getDrive().files().listFolder(parentId);
    while (true) {
      parent.addChildren(result.getEntries().stream().map(this::buildTreeNode).collect(Collectors.toSet()));
      if (!result.getHasMore()) {
        break;
      }
      result = getDrive().files().listFolderContinue(result.getCursor());
    }
    for (TreeNode child : parent.getChildren()) {
      if (child.getAttributes().isDir()) {
        buildTreeNodeRecursively(child.getId(), child);
      }
    }
  }

  @Override
  @SneakyThrows
  public Set<TreeNode> loadTreeUpToChild(@Nullable String rootPath, @NotNull String id) {
    Metadata metadata = getDrive().files().getMetadata(id);
    if (metadata == null) {
      return null;
    }
    return getChildren("");
  }

  @Override
  @SneakyThrows
  public @NotNull Set<TreeNode> getChildren(@NotNull String parentId) {
    ListFolderResult result = getDrive().files().listFolder(parentId);
    Set<TreeNode> files = new HashSet<>();
    while (true) {
      files.addAll(result.getEntries().stream().map(this::buildTreeNode).collect(Collectors.toSet()));
      if (!result.getHasMore()) {
        break;
      }
      result = getDrive().files().listFolderContinue(result.getCursor());
    }
    return files;
  }

  @Override
  public TreeNode copy(@NotNull Collection<TreeNode> entries, @NotNull String targetId, UploadOption uploadOption) {
    List<Metadata> result = new ArrayList<>();
    copyEntries(entries, targetId, uploadOption, result);
    return buildRoot(result);
  }

  @SneakyThrows
  private void copyEntries(Collection<TreeNode> entries, String targetId, UploadOption uploadOption, List<Metadata> result) {
    for (TreeNode entry : entries) {
      String path = Paths.get(targetId).resolve(entry.getName()).toString();

      if (!entry.getAttributes().isDir()) {
        try (InputStream stream = entry.getInputStream()) {
          if (uploadOption == UploadOption.Append) {
            byte[] prependContent = IOUtils.toByteArray(getDrive().files().download(path).getInputStream());
            byte[] content = Bytes.concat(prependContent, IOUtils.toByteArray(entry.getInputStream()));
            result.add(getDrive().files().uploadBuilder(path).uploadAndFinish(new ByteArrayInputStream(content)));
          } else {
            result.add(getDrive().files().uploadBuilder(path).uploadAndFinish(stream));
          }
        }
      } else {
        result.add(getDrive().files().createFolderV2(path).getMetadata());
        copyEntries(entry.getFileSystem().getChildren(entry), path, uploadOption, result);
      }
    }
  }

  private TreeNode buildTreeNode(Metadata metadata) {
    if (metadata instanceof FolderMetadata fm) {
      boolean hasChildren = true;
      TreeNode node = new TreeNode(true, !hasChildren,
          fm.getName(), fm.getId(), null, null, this, null);
      node.getAttributes().setReadOnly(isReadOnly(fm.getSharingInfo()));
      return node;
    }
    FileMetadata file = (FileMetadata) metadata;
    MediaInfo mediaInfo = file.getMediaInfo();
    TreeNode node = new TreeNode(false, true,
        file.getName(), file.getId(), file.getSize(), file.getServerModified().getTime(), this,
        mediaInfo == null ? null : mediaInfo.toString());
    node.getAttributes().setReadOnly(isReadOnly(file.getSharingInfo()));
    return node;
  }

  private boolean isReadOnly(SharingInfo sharingInfo) {
    return sharingInfo != null && sharingInfo.getReadOnly();
  }

  private TreeNode buildRoot(Collection<Metadata> files) {
    throw new RuntimeException("!!!!!!!!!!");
  }

  @SneakyThrows
  private DbxClientV2 getDrive() {
    if (drive == null) {
      drive = new DbxClientV2(config, entity.getDropboxApiToken().asString());
      drive.users().getCurrentAccount();
    }
    return drive;
  }
}
