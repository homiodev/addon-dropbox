package org.homio.addon.dropbox;

import jakarta.persistence.Entity;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.homio.addon.dropbox.DropboxEntity.DropboxService;
import org.homio.api.Context;
import org.homio.api.entity.storage.BaseFileSystemEntity;
import org.homio.api.entity.types.StorageEntity;
import org.homio.api.model.Icon;
import org.homio.api.service.EntityService;
import org.homio.api.ui.UISidebarChildren;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.action.v1.UIInputBuilder;
import org.homio.api.util.Lang;
import org.homio.api.util.SecureString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"JpaAttributeTypeInspection", "JpaAttributeMemberSignatureInspection"})
@Entity
@UISidebarChildren(icon = "fab fa-dropbox", color = "#536AC5")
public class DropboxEntity extends StorageEntity
    implements BaseFileSystemEntity<DropboxEntity, DropboxFileSystem>,
    EntityService<DropboxService> {

  @UIField(order = 30, required = true, inlineEditWhenEmpty = true)
  public SecureString getDropboxApiToken() {
    return getJsonSecure("apiToken");
  }

  public void setDropboxApiToken(String value) {
    setJsonData("apiToken", value);
  }

  @UIField(order = 40, hideInEdit = true)
  public String getUsedSpace() {
    try {
      return humanReadableByteCountSI(getFileSystem(context()).getUsedSpace());
    } catch (Exception ignore) {return "---";}
  }

  @UIField(order = 50, hideInEdit = true)
  public String getTotalSpace() {
    try {
      return humanReadableByteCountSI(getFileSystem(context()).getTotalSpace());
    } catch (Exception ignore) {return "---";}
  }

  @Override
  public String getDescriptionImpl() {
    if (getDropboxApiToken().isEmpty()) {
      return Lang.getServerMessage("DROPBOX.DESCRIPTION");
    }
    return null;
  }

  @Override
  public @Nullable Set<String> getConfigurationErrors() {
    if (getDropboxApiToken().isEmpty()) {
      return Set.of("ERROR.NO_API_TOKEN");
    }
    return null;
  }

  @Override
  public long getEntityServiceHashCode() {
    return getEntityID().hashCode() + getDropboxApiToken().asString().hashCode();
  }

  @Override
  public @NotNull Class<DropboxService> getEntityServiceItemClass() {
    return DropboxService.class;
  }

  @Override
  public @Nullable DropboxService createService(@NotNull Context context) {
    return new DropboxService(context, this);
  }

  @Override
  protected @NotNull String getDevicePrefix() {
    return "dropbox_";
  }

  @Override
  public @NotNull String getFileSystemRoot() {
    return "/";
  }

  @Override
  public @NotNull String getFileSystemAlias() {
    return "DROPBOX";
  }

  @Override
  public boolean isShowInFileManager() {
    return true;
  }

  @Override
  public @NotNull Icon getFileSystemIcon() {
    return new Icon("fab fa-dropbox", "#536AC5");
  }

  @Override
  public boolean requireConfigure() {
    return StringUtils.isEmpty(getDropboxApiToken());
  }

  @Override
  public @NotNull DropboxFileSystem buildFileSystem(@NotNull Context context) {
    return new DropboxFileSystem(this);
  }

  @Override
  public long getConnectionHashCode() {
    return Objects.hash(getDropboxApiToken().asString());
  }

  @Override
  public boolean isShowHiddenFiles() {
    return true;
  }

  @Override
  public String getDefaultName() {
    return "Dropbox";
  }

  @Override
  public void assembleActions(UIInputBuilder uiInputBuilder) {

  }

  public static class DropboxService extends EntityService.ServiceInstance<DropboxEntity> {

    public DropboxService(Context context, DropboxEntity entity) {
      super(context, entity, true);
    }

    @Override
    protected void initialize() {
      testServiceWithSetStatus();
    }

    @Override
    public void testService() {
      entity.getFileSystem(context).getAbout();
    }

    @Override
    public void destroy(boolean forRestart, Exception ex) {

    }
  }

  private static String humanReadableByteCountSI(long bytes) {
    if (-1000 < bytes && bytes < 1000) {
      return bytes + " B";
    }
    CharacterIterator ci = new StringCharacterIterator("kMGTPE");
    while (bytes <= -999_950 || bytes >= 999_950) {
      bytes /= 1000;
      ci.next();
    }
    return String.format("%.1f %cB", bytes / 1000.0, ci.current());
  }
}
