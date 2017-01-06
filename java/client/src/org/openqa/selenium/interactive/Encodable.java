package org.openqa.selenium.interactive;

import java.util.Map;

/**
 * This interface allows a custom {@link Interaction} to be JSON encoded for the W3C wire format. It
 * should not normally be exposed or used by user-facing APIs. Instead, these should traffic in the
 * {@link Interaction} interface.
 */
public interface Encodable {
  Map<String, Object> encode();
}
