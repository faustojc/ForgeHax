package com.matt.forgehax.gui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ModalGeometryTest {

  @Test
  public void centersAndCapsLargeScreens() {
    ModalGeometry geometry = ModalGeometry.calculate(1920, 1080);

    assertEquals(960, geometry.getWidth());
    assertEquals(560, geometry.getHeight());
    assertEquals(480, geometry.getX());
    assertEquals(260, geometry.getY());
    assertEquals(34, geometry.getHeaderHeight());
    assertEquals(220, geometry.getSidebarWidth());
  }

  @Test
  public void preservesMarginsOnSmallScreens() {
    ModalGeometry geometry = ModalGeometry.calculate(320, 240);

    assertEquals(296, geometry.getWidth());
    assertEquals(216, geometry.getHeight());
    assertEquals(12, geometry.getX());
    assertEquals(12, geometry.getY());
    assertEquals(120, geometry.getSidebarWidth());
    assertTrue(geometry.getSettings().getWidth() > 0);
  }
}
