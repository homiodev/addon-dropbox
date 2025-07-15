package org.homio.addon.dropbox;

import jakarta.persistence.Entity;
import org.apache.commons.lang3.StringUtils;
import org.homio.addon.dropbox.DropboxEntity.DropboxService;
import org.homio.api.Context;
import org.homio.api.entity.device.DeviceBaseEntity;
import org.homio.api.entity.storage.BaseFileSystemEntity;
import org.homio.api.service.EntityService;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.action.v1.UIInputBuilder;
import org.homio.api.ui.route.UIRouteStorage;
import org.homio.api.util.Lang;
import org.homio.api.util.SecureString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

@SuppressWarnings({"JpaAttributeTypeInspection", "JpaAttributeMemberSignatureInspection", "unused"})
@Entity
@UIRouteStorage(icon = "fab fa-dropbox", color = "#536AC5")
public class DropboxEntity extends DeviceBaseEntity
  implements BaseFileSystemEntity<DropboxFileSystem>,
  EntityService<DropboxService> {

  @UIField(order = 30, required = true, inlineEditWhenEmpty = true)
  public SecureString getDropboxApiToken() {
    return getJsonSecure("apiToken");
  }

  public void setDropboxApiToken(String value) {
    setJsonData("apiToken", value);
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
    return getConnectionHashCode();
  }

  @Override
  public @Nullable DropboxService createService(@NotNull Context context) {
    return new DropboxService(context, this);
  }

  @Override
  protected @NotNull String getDevicePrefix() {
    return "dropbox";
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
  public boolean requireConfigure() {
    return StringUtils.isEmpty(getDropboxApiToken());
  }

  @Override
  public @NotNull DropboxFileSystem buildFileSystem(@NotNull Context context, int alias) {
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
      super(context, entity, true, "Dripbox");
    }

    @Override
    protected void initialize() {
      testServiceWithSetStatus();
    }

    @Override
    public void testService() {
      entity.getFileSystem(context, 0).getAbout();
    }

    @Override
    public void destroy(boolean forRestart, Exception ex) {

    }
  }
}
