package org.openqa.selenium.interactive;

import com.google.common.base.Preconditions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Pause extends Interaction implements Encodable {

  private final Duration duration;

  protected Pause(InputDevice device, Duration duration) {
    super(device);

    Preconditions.checkState(!duration.isNegative(), "Duration must be set to 0 or more: %s", duration);
    this.duration = duration;
  }

  @Override
  protected boolean isValidFor(SourceType sourceType) {
    return true;
  }

  @Override
  public Map<String, Object> encode() {
    Map<String, Object> toReturn = new HashMap<>();

    toReturn.put("type", "pause");
    toReturn.put("duration", duration.toMillis());

    return toReturn;
  }
}
