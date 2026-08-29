package com.matt.forgehax.gui.components;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GuiScrollModelTest {

  @Test
  public void clampsOffsetToContent() {
    GuiScrollModel scroll = new GuiScrollModel();
    scroll.setExtent(300, 100);

    scroll.setOffset(250);
    assertEquals(200, scroll.getOffset());

    scroll.scrollBy(-500);
    assertEquals(0, scroll.getOffset());
  }

  @Test
  public void reclampsWhenViewportChanges() {
    GuiScrollModel scroll = new GuiScrollModel();
    scroll.setExtent(300, 100);
    scroll.scrollToBottom();
    assertEquals(200, scroll.getOffset());

    scroll.setExtent(300, 250);
    assertEquals(50, scroll.getOffset());
  }
}
