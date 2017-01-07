package org.openqa.selenium.interactive;

import org.openqa.selenium.Beta;

import java.util.List;

/**
 * Interface to help us transition code to The New World
 */
@Beta
public interface IsInteraction {
  List<Interaction> asInteractions(PointerInput mouse, KeyInput keyboard);
}
