package org.openqa.selenium.interactive;

import java.util.HashMap;
import java.util.Map;

public class KeyInput implements InputDevice, Encodable {

  @Override
  public SourceType getInputType() {
    return SourceType.KEY;
  }

  @Override
  public Map<String, Object> encode() {

    return new HashMap<>();
  }
}
