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

package org.openqa.selenium.interactive;


import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.BeanToJsonConverter;

import java.util.List;
import java.util.Map;

public class InteractionsTest {

  @Test
  public void simpleInteractions() {

    WebElement element = new WebElement() {
      @Override
      public void click() {

      }

      @Override
      public void submit() {

      }

      @Override
      public void sendKeys(CharSequence... keysToSend) {

      }

      @Override
      public void clear() {

      }

      @Override
      public String getTagName() {
        return null;
      }

      @Override
      public String getAttribute(String name) {
        return null;
      }

      @Override
      public boolean isSelected() {
        return false;
      }

      @Override
      public boolean isEnabled() {
        return false;
      }

      @Override
      public String getText() {
        return null;
      }

      @Override
      public List<WebElement> findElements(By by) {
        return null;
      }

      @Override
      public WebElement findElement(By by) {
        return null;
      }

      @Override
      public boolean isDisplayed() {
        return false;
      }

      @Override
      public Point getLocation() {
        return null;
      }

      @Override
      public Dimension getSize() {
        return null;
      }

      @Override
      public Rectangle getRect() {
        return null;
      }

      @Override
      public String getCssValue(String propertyName) {
        return null;
      }

      @Override
      public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        return null;
      }
    };

    Map<String, Object> foo = new Actions(new ChromeDriver())
        .click(element)
        .toJson();

    String converted = new BeanToJsonConverter().convert(foo);

    System.out.println(converted);
  }

//  @Test
//  public void usingParticularInputDevices() {
//    MouseInput mouse = new MouseInput("mouse1");
//    new Interactions(driver)
//        .tick(mouse.moveTo(target))
//        .tick(mouse.pointerDown())
//        .tick(mouse.pointerUp())
//        .perform();
//
//  }
}
