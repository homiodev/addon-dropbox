package org.touchhome.bundle.dropbox;

import java.util.Objects;
import javax.persistence.Entity;
import org.apache.commons.lang3.StringUtils;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.entity.storage.BaseFileSystemEntity;
import org.touchhome.bundle.api.entity.types.StorageEntity;
import org.touchhome.bundle.api.ui.UISidebarChildren;
import org.touchhome.bundle.api.ui.field.UIField;
import org.touchhome.bundle.api.ui.field.action.v1.UIInputBuilder;
import org.touchhome.bundle.api.util.SecureString;

@Entity
@UISidebarChildren(icon = "fab fa-dropbox", color = "#0d2481")
public class DropboxEntity extends StorageEntity<DropboxEntity>
    implements BaseFileSystemEntity<DropboxEntity, DropboxFileSystem> {

  public static final String PREFIX = "dropbox_";

  @UIField(order = 30, required = true, inlineEditWhenEmpty = true)
  public SecureString getDropboxApiToken() {
    return getJsonSecure("apiToken");
  }

  public void setDropboxApiToken(String value) {
    setJsonData("apiToken", value);
  }

  @Override
  public String getEntityPrefix() {
    return PREFIX;
  }

  @Override
  public String getFileSystemAlias() {
    return "DROPBOX";
  }

  @Override
  public boolean isShowInFileManager() {
    return true;
  }

  @Override
  public String getFileSystemIcon() {
    return "fab fa-dropbox";
  }

  @Override
  public String getFileSystemIconColor() {
    return "#0d2481";
  }

  @Override
  public boolean requireConfigure() {
    return StringUtils.isEmpty(getDropboxApiToken());
  }

  @Override
  public DropboxFileSystem buildFileSystem(EntityContext entityContext) {
    return new DropboxFileSystem(this);
  }

  @Override
  public long getConnectionHashCode() {
    return Objects.hash(getDropboxApiToken().asString());
  }

  @Override
  public String getDefaultName() {
    return "Dropbox";
  }

  @Override
  public void assembleActions(UIInputBuilder uiInputBuilder) {

  }
}
