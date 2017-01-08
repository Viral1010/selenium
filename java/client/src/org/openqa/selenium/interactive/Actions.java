package org.openqa.selenium.interactive;

import static org.openqa.selenium.interactive.PointerInput.Kind.MOUSE;

import com.google.common.base.Preconditions;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;
import org.openqa.selenium.interactions.internal.MouseAction.Button;
import org.openqa.selenium.interactive.PointerInput.MouseButton;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * The user-facing API for emulating complex user gestures. Use this class rather than using the
 * Keyboard or Mouse directly.
 * <p>
 * Implements the builder pattern: Builds a CompositeAction containing all actions specified by the
 * method calls.
 */
public class Actions {

  private final Map<InputDevice, Sequence> sequences = new HashMap<>();
  private final PointerInput defaultMouse = new PointerInput(
      MOUSE,
      Optional.of("default mouse"),
      /* primary */ true);
  private final KeyInput defaultKeyboard = new KeyInput();
  private final WebDriver driver;

  public Actions(WebDriver driver) {
    this.driver = Preconditions.checkNotNull(driver);
  }

  /**
   * Performs a modifier key press. Does not release the modifier key - subsequent interactions
   * may assume it's kept pressed.
   * Note that the modifier key is <b>never</b> released implicitly - either
   * <i>keyUp(theKey)</i> or <i>sendKeys(Keys.NULL)</i>
   * must be called to release the modifier.
   * @param key Either {@link Keys#SHIFT}, {@link Keys#ALT} or {@link Keys#CONTROL}. If the
   * provided key is none of those, {@link IllegalArgumentException} is thrown.
   * @return A self reference.
   */
  public Actions keyDown(CharSequence key) {
    return addKeyAction(key, codePoint -> tick(defaultKeyboard.createKeyDown(codePoint)));
  }

  /**
   * Performs a modifier key press after focusing on an element. Equivalent to:
   * <i>Actions.click(element).sendKeys(theKey);</i>
   * @see #keyDown(CharSequence)
   *
   * @param key Either {@link Keys#SHIFT}, {@link Keys#ALT} or {@link Keys#CONTROL}. If the
   * provided key is none of those, {@link IllegalArgumentException} is thrown.
   * @param target WebElement to perform the action
   * @return A self reference.
   */
  public Actions keyDown(WebElement target, CharSequence key) {
    return click(target).keyDown(key);
  }

  /**
   * Performs a modifier key release. Releasing a non-depressed modifier key will yield undefined
   * behaviour.
   *
   * @param key Either {@link Keys#SHIFT}, {@link Keys#ALT} or {@link Keys#CONTROL}.
   * @return A self reference.
   */
  public Actions keyUp(CharSequence key) {
    return addKeyAction(key, codePoint -> tick(defaultKeyboard.createKeyUp(codePoint)));
  }

  /**
   * Performs a modifier key release after focusing on an element. Equivalent to:
   * <i>Actions.click(element).sendKeys(theKey);</i>
   * @see #keyUp(CharSequence) on behaviour regarding non-depressed modifier keys.
   *
   * @param key Either {@link Keys#SHIFT}, {@link Keys#ALT} or {@link Keys#CONTROL}.
   * @param target WebElement to perform the action on
   * @return A self reference.
   */
  public Actions keyUp(WebElement target, CharSequence key) {
    return  click(target).keyUp(key);
  }

  /**
   * Sends keys to the active element. This differs from calling
   * {@link WebElement#sendKeys(CharSequence...)} on the active element in two ways:
   * <ul>
   * <li>The modifier keys included in this call are not released.</li>
   * <li>There is no attempt to re-focus the element - so sendKeys(Keys.TAB) for switching
   * elements should work. </li>
   * </ul>
   *
   * @see WebElement#sendKeys(CharSequence...)
   *
   * @param keys The keys.
   * @return A self reference.
   */
  public Actions sendKeys(CharSequence... keys) {
    for (CharSequence key : keys) {
      key.codePoints().forEach(codePoint -> {
        tick(defaultKeyboard.createKeyDown(codePoint));
        tick(defaultKeyboard.createKeyUp(codePoint));
      });
    }

    return this;
  }

  /**
   * Equivalent to calling:
   * <i>Actions.click(element).sendKeys(keysToSend).</i>
   * This method is different from {@link WebElement#sendKeys(CharSequence...)} - see
   * {@link #sendKeys(CharSequence...)} for details how.
   *
   * @see #sendKeys(java.lang.CharSequence[])
   *
   * @param target element to focus on.
   * @param keys The keys.
   * @return A self reference.
   */
  public Actions sendKeys(WebElement target, CharSequence... keys) {
    return click(target).sendKeys(keys);
  }

  private Actions addKeyAction(CharSequence key, IntConsumer consumer) {
    // Verify that we only have a single character to type.
    Preconditions.checkState(
        key.codePoints().count() == 1,
        "Only one code point is allowed at a time: %s", key);

    key.codePoints().forEach(consumer);

    return this;
  }

  /**
   * Clicks (without releasing) in the middle of the given element. This is equivalent to:
   * <i>Actions.moveToElement(onElement).clickAndHold()</i>
   *
   * @param target Element to move to and click.
   * @return A self reference.
   */
  public Actions clickAndHold(WebElement target) {
    tick(defaultMouse.createPointerMove(Duration.ofMillis(500), target, 1, 1));
    tick(defaultMouse.createPointerDown(MouseButton.LEFT.asArg()));
    return this;
  }

  /**
   * Clicks (without releasing) at the current mouse location.
   * @return A self reference.
   */
  public Actions clickAndHold() {
    tick(defaultMouse.createPointerDown(MouseButton.LEFT.asArg()));
    return this;
  }

  /**
   * Releases the depressed left mouse button, in the middle of the given element.
   * This is equivalent to:
   * <i>Actions.moveToElement(onElement).release()</i>
   *
   * Invoking this action without invoking {@link #clickAndHold()} first will result in
   * undefined behaviour.
   *
   * @param target Element to release the mouse button above.
   * @return A self reference.
   */
  public Actions release(WebElement target) {
    return moveToElement(target).release();
  }

  /**
   * Releases the depressed left mouse button at the current mouse location.
   * @see #release(org.openqa.selenium.WebElement)
   * @return A self reference.
   */
  public Actions release() {
    return tick(defaultMouse.createPointerUp(Button.LEFT.asArg()));
  }

  /**
   * Clicks in the middle of the given element. Equivalent to:
   * <i>Actions.moveToElement(onElement).click()</i>
   *
   * @param target Element to click.
   * @return A self reference.
   */
  public Actions click(WebElement target) {
    return tick(defaultMouse.createPointerMove(Duration.ofMillis(250), target, 1, 1))
        .click();
  }

  /**
   * Clicks at the current mouse location. Useful when combined with
   * {@link #moveToElement(org.openqa.selenium.WebElement, int, int)} or
   * {@link #moveByOffset(int, int)}.
   * @return A self reference.
   */
  public Actions click() {
    tick(defaultMouse.createPointerDown(0));
    tick(defaultMouse.createPointerUp(0));

    return this;
  }

  /**
   * Performs a double-click at middle of the given element. Equivalent to:
   * <i>Actions.moveToElement(element).doubleClick()</i>
   *
   * @param onElement Element to move to.
   * @return A self reference.
   */
  public Actions doubleClick(WebElement onElement) {
    return moveToElement(onElement).doubleClick();
  }

  /**
   * Performs a double-click at the current mouse location.
   * @return A self reference.
   */
  public Actions doubleClick() {
    return click().click();
  }

  /**
   * Moves the mouse to the middle of the element. The element is scrolled into view and its
   * location is calculated using getBoundingClientRect.
   * @param toElement element to move to.
   * @return A self reference.
   */
  public Actions moveToElement(WebElement toElement) {
    return moveToElement(toElement, 1, 1);
  }

  /**
   * Moves the mouse to an offset from the top-left corner of the element.
   * The element is scrolled into view and its location is calculated using getBoundingClientRect.
   * @param toElement element to move to.
   * @param xOffset Offset from the top-left corner. A negative value means coordinates left from
   * the element.
   * @param yOffset Offset from the top-left corner. A negative value means coordinates above
   * the element.
   * @return A self reference.
   */
  public Actions moveToElement(WebElement toElement, int xOffset, int yOffset) {
    return tick(
        defaultMouse.createPointerMove(Duration.ofMillis(250), toElement, xOffset, yOffset));
  }

  /**
   * Moves the mouse from its current position (or 0,0) by the given offset. If the coordinates
   * provided are outside the viewport (the mouse will end up outside the browser window) then
   * the viewport is scrolled to match.
   * @param xOffset horizontal offset. A negative value means moving the mouse left.
   * @param yOffset vertical offset. A negative value means moving the mouse up.
   * @return A self reference.
   * @throws MoveTargetOutOfBoundsException if the provided offset is outside the document's
   * boundaries.
   */
  public Actions moveByOffset(int xOffset, int yOffset) {
    return  tick(defaultMouse.createPointerMove(Duration.ofMillis(200), null, xOffset, yOffset));
  }

  /**
   * Performs a context-click at middle of the given element. First performs a mouseMove
   * to the location of the element.
   *
   * @param target Element to move to.
   * @return A self reference.
   */
  public Actions contextClick(WebElement target) {
    return moveToElement(target).contextClick();
  }

  /**
   * Performs a context-click at the current mouse location.
   * @return A self reference.
   */
  public Actions contextClick() {
    return tick(defaultMouse.createPointerDown(Button.RIGHT.asArg()))
        .tick(defaultMouse.createPointerUp(Button.RIGHT.asArg()));
  }

  /**
   * A convenience method that performs click-and-hold at the location of the source element,
   * moves to the location of the target element, then releases the mouse.
   *
   * @param source element to emulate button down at.
   * @param target element to move to and release the mouse at.
   * @return A self reference.
   */
  public Actions dragAndDrop(WebElement source, WebElement target) {
    return clickAndHold(source).moveToElement(target).release();
  }

  /**
   * A convenience method that performs click-and-hold at the location of the source element,
   * moves by a given offset, then releases the mouse.
   *
   * @param source element to emulate button down at.
   * @param xOffset horizontal move offset.
   * @param yOffset vertical move offset.
   * @return A self reference.
   */
  public Actions dragAndDropBy(WebElement source, int xOffset, int yOffset) {
    return clickAndHold(source).moveByOffset(xOffset, yOffset).release();
  }

  /**
   * Performs a pause.
   *
   * @param pause pause duration, in milliseconds.
   * @return A self reference.
   *
   * @deprecated 'Pause' is considered to be a bad design practice.
   */
  @Deprecated
  public Actions pause(long pause) {
    return tick(new Pause(defaultMouse, Duration.ofMillis(pause)));
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

  public Actions tick(Action action) {
    Preconditions.checkState(action instanceof IsInteraction);

    for (Interaction interaction :
        ((IsInteraction) action).asInteractions(defaultMouse, defaultKeyboard)) {
      tick(interaction);
    }

    return this;
  }

  public void perform() {
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
}
