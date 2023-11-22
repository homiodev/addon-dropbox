package org.homio.addon.dropbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.homio.api.AddonEntrypoint;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class DropboxEntrypoint implements AddonEntrypoint {

  public void init() {
  }
}
