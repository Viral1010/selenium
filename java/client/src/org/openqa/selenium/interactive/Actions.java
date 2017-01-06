package org.openqa.selenium.interactive;

import static org.openqa.selenium.interactive.PointerInput.Kind.MOUSE;

import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Actions {

  private final Map<InputDevice, Sequence> sequences = new HashMap<>();
  private final PointerInput defaultMouse = new PointerInput(
      MOUSE,
      Optional.of("default mouse"),
      /* primary */ true);

  public Actions click(WebElement target) {
    tick(defaultMouse.createPointerMove(Duration.ofMillis(250), target, 1, 1));
    tick(defaultMouse.createPointerDown(0));
    tick(defaultMouse.createPointerUp(0));

    return this;
  }

  public Actions tick(Interaction... actions) {
    // All actions must be for a unique device.
    Set<InputDevice> seenDevices = new HashSet<>();
    for (Interaction action : actions) {
      boolean freshlyAdded = seenDevices.add(action.getSource());
      if (!freshlyAdded) {
        throw new IllegalStateException(String.format(
            "You may only add one action per input device per tick: %s",
            Arrays.asList(actions)));
      }
    }

    // Add all actions to sequences
    for (Interaction action : actions) {
      Sequence sequence = getSequence(action.getSource());
      sequence.addAction(action);
      seenDevices.remove(action.getSource());
    }

    // And now pad the remaining sequences with a pause.
    for (InputDevice device : seenDevices) {
      getSequence(device).addAction(new Pause(device, Duration.ZERO));
    }

    return this;
  }

  private Sequence getSequence(InputDevice device) {
    Sequence sequence = sequences.get(device);
    if (sequence != null) {
      return sequence;
    }

    int longest = 0;
    for (Sequence examining : sequences.values()) {
      longest = Math.max(longest, examining.size());
    }

    sequence = new Sequence(device, longest);
    sequences.put(device, sequence);

    return sequence;
  }

  public Map<String, Object> toJson()  {
    List<Map<String, Object>> encodedSequences = new ArrayList<>(sequences.keySet().size());
    for (Sequence sequence : sequences.values()) {
      encodedSequences.add(sequence.encode());
    }

    Map<String, Object> toReturn = new HashMap<>();
    toReturn.put("actions", encodedSequences);

    return toReturn;
  }
}
