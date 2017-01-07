package org.openqa.selenium.interactive;

import java.util.HashMap;
import java.util.Map;

public class KeyInput implements InputDevice, Encodable {

  @Override
  public SourceType getInputType() {
    return SourceType.KEY;
  }

  public Interaction createKeyDown(int codePoint) {
    return new TypingInteraction(this, "keyDown", codePoint);
  }

  public Interaction createKeyUp(int codePoint) {
    return new TypingInteraction(this, "keyUp", codePoint);
  }

  @Override
  public Map<String, Object> encode() {
    return new HashMap<>();
  }

  private static class TypingInteraction extends Interaction implements Encodable {

    private final String type;
    private final String value;

    TypingInteraction(InputDevice source, String type, int codePoint) {
      super(source);

      this.type = type;
      this.value = new StringBuilder().appendCodePoint(codePoint).toString();
    }

    @Override
    public Map<String, Object> encode() {
      HashMap<String, Object> toReturn = new HashMap<>();

      toReturn.put("type", type);
      toReturn.put("value", value);

      return toReturn;
    }
  }
}
