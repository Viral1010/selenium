// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.interactions;

import static org.openqa.selenium.interactions.PointerInput.Kind.MOUSE;
import static org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT;

import com.google.common.base.Preconditions;

import org.openqa.selenium.Keys;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.internal.MouseAction.Button;
import org.openqa.selenium.internal.Locatable;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

  private final WebDriver driver;

  // W3C
  private final Map<InputDevice, Sequence> sequences = new HashMap<>();
  private final PointerInput defaultMouse = new PointerInput(
      MOUSE,
      Optional.of("default mouse"),
      /* primary */ true);
  private final KeyInput defaultKeyboard = new KeyInput();

  // JSON-wire protocol
  private final Keyboard jsonKeyboard;
  private final Mouse jsonMouse;
  protected final CompositeAction action = new CompositeAction();
  private RuntimeException actionsException;

  public Actions(WebDriver driver) {
    this.driver = Preconditions.checkNotNull(driver);

    if (driver instanceof HasInputDevices) {
      HasInputDevices deviceOwner = (HasInputDevices) driver;
      this.jsonKeyboard = deviceOwner.getKeyboard();
      this.jsonMouse = deviceOwner.getMouse();
    } else {
      this.jsonKeyboard = null;
      this.jsonMouse = null;
    }
  }

  /**
   * A constructor that should only be used when the keyboard or mouse were extended to provide
   * additional functionality (for example, dragging-and-dropping from the desktop).
   * @param keyboard the {@link Keyboard} implementation to delegate to.
   * @param mouse the {@link Mouse} implementation to delegate to.
   * @deprecated Use the new interactions APIs.
   */
  @Deprecated
  public Actions(Keyboard keyboard, Mouse mouse) {
    this.driver = null;
    this.jsonKeyboard = keyboard;
    this.jsonMouse = mouse;
  }

  /**
   * Only used by the TouchActions class.
   * @param keyboard implementation to delegate to.
   * @deprecated Use the new interactions API.
   */
  @Deprecated
  public Actions(Keyboard keyboard) {
    this.driver = null;
    this.jsonKeyboard = keyboard;
    this.jsonMouse = null;
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
    if (isBuildingActions()) {
      if (!(key instanceof Keys)) {
        actionsException = new IllegalArgumentException(
            "keyDown argument must be an instanceof Keys: " + key);
      } else {
        action.addAction(new KeyDownAction(jsonKeyboard, jsonMouse, null, (Keys) key));
      }
    }
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
    if (isBuildingActions()) {
      if (!(key instanceof Keys)) {
        actionsException = new IllegalArgumentException(
            "keyDown argument must be an instanceof Keys: " + key);
      } else {
        action.addAction(new KeyUpAction(
            jsonKeyboard,
            jsonMouse,
            null,
            (Keys) key));
      }
    }

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
    return click(target).keyUp(key);
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
    if (isBuildingActions()) {
      action.addAction(new SendKeysAction(jsonKeyboard, jsonMouse, null, keys));
    }

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
    return moveToElement(target).clickAndHold();
  }

  /**
   * Clicks (without releasing) at the current mouse location.
   * @return A self reference.
   */
  public Actions clickAndHold() {
    if (isBuildingActions()) {
      action.addAction(new ClickAndHoldAction(jsonMouse, null));
    }

    tick(defaultMouse.createPointerDown(LEFT.asArg()));
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
    if (isBuildingActions()) {
      action.addAction(new ButtonReleaseAction(jsonMouse, null));
    }

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
    return moveToElement(target).click();
  }

  /**
   * Clicks at the current mouse location. Useful when combined with
   * {@link #moveToElement(org.openqa.selenium.WebElement, int, int)} or
   * {@link #moveByOffset(int, int)}.
   * @return A self reference.
   */
  public Actions click() {
    if (isBuildingActions()) {
      action.addAction(new ClickAction(jsonMouse, null));
    }
    tick(defaultMouse.createPointerDown(0));
    tick(defaultMouse.createPointerUp(0));

    return this;
  }

  /**
   * Performs a double-click at middle of the given element. Equivalent to:
   * <i>Actions.moveToElement(element).doubleClick()</i>
   *
   * @param target Element to move to.
   * @return A self reference.
   */
  public Actions doubleClick(WebElement target) {
    return moveToElement(target).doubleClick();
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
   * @param target element to move to.
   * @return A self reference.
   */
  public Actions moveToElement(WebElement target) {
    return moveToElement(target, 1, 1);
  }

  /**
   * Moves the mouse to an offset from the top-left corner of the element.
   * The element is scrolled into view and its location is calculated using getBoundingClientRect.
   * @param target element to move to.
   * @param xOffset Offset from the top-left corner. A negative value means coordinates left from
   * the element.
   * @param yOffset Offset from the top-left corner. A negative value means coordinates above
   * the element.
   * @return A self reference.
   */
  public Actions moveToElement(WebElement target, int xOffset, int yOffset) {
    if (isBuildingActions()) {
      action.addAction(new MoveToOffsetAction(jsonMouse, (Locatable) target, xOffset, yOffset));
    }

    return tick(
        defaultMouse.createPointerMove(Duration.ofMillis(250), target, xOffset, yOffset));
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
    if (isBuildingActions()) {
      action.addAction(new MoveToOffsetAction(jsonMouse, null, xOffset, yOffset));
    }

    return tick(
        defaultMouse.createPointerMove(Duration.ofMillis(200), null, xOffset, yOffset));
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
    if (isBuildingActions()) {
      action.addAction(new ContextClickAction(jsonMouse, null));
    }

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
    if (isBuildingActions()) {
      action.addAction(new PauseAction(pause));
    }

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

    if (isBuildingActions()) {
      actionsException = new IllegalArgumentException(
          "You may not use new style interactions with old style actions");
    }

    return this;
  }

  public Actions tick(Action action) {
    Preconditions.checkState(action instanceof IsInteraction);

    for (Interaction interaction :
        ((IsInteraction) action).asInteractions(defaultMouse, defaultKeyboard)) {
      tick(interaction);
    }

    if (isBuildingActions()) {
      this.action.addAction(action);
    }

    return this;
  }

  /**
   * A no-op left for legacy reasons.
   * @deprecated Use {@link #perform()} directly.
   */
  @Deprecated
  public Action build() {
    return this::perform;
  }

  /**
   * Execute all the actions this represents. Does not reset state after calling.
   */
  public void perform() {
    try {
      ((Interactive) driver).perform(sequences.values());
    } catch (ClassCastException | UnsupportedCommandException e) {
      // Fall back to the old way of doing things. Old Skool #ftw
      action.perform();
    }
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

  private boolean isBuildingActions() {
    return jsonMouse != null || jsonKeyboard != null;
  }
}
