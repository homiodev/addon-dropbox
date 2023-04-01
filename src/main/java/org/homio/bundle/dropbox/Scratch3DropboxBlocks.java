package org.homio.bundle.dropbox;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.homio.bundle.api.EntityContext;
import org.homio.bundle.api.entity.storage.Scratch3BaseFileSystemExtensionBlocks;

@Getter
@Component
public class Scratch3DropboxBlocks extends Scratch3BaseFileSystemExtensionBlocks<DropboxEntrypoint, DropboxEntity> {

  public Scratch3DropboxBlocks(EntityContext entityContext, DropboxEntrypoint dropboxEntrypoint) {
    super("#355279", entityContext, dropboxEntrypoint, DropboxEntity.class);
  }
}
