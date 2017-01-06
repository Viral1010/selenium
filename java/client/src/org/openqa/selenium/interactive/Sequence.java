package org.openqa.selenium.interactive;

import com.google.common.base.Preconditions;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class Sequence implements Encodable {

  private final List<Encodable> actions = new LinkedList<>();
  private final InputDevice device;

  public Sequence(InputDevice device, int initialLength) {
    Preconditions.checkState(device instanceof Encodable);

    this.device = device;

    for (int i = 0; i < initialLength; i++) {
      addAction(new Pause(device, Duration.ZERO));
    }
  }

  public Sequence addAction(Interaction action) {
    Preconditions.checkState(action.isValidFor(device.getInputType()));
    Preconditions.checkNotNull(action instanceof Encodable);

    actions.add((Encodable) action);

    return this;
  }

  @Override
  public Map<String, Object> encode() {
    Map<String, Object> toReturn = new HashMap<>();
    toReturn.putAll(((Encodable) device).encode());

    List<Map<String, Object>> encodedActions = new LinkedList<>();
    for (Encodable action : actions) {
      Map<String, Object> encodedAction = new HashMap<>();
      encodedAction.putAll(action.encode());
      encodedActions.add(encodedAction);
    }
    toReturn.put("actions", encodedActions);

    return toReturn;
  }

  int size() {
    return actions.size();
  }
}
