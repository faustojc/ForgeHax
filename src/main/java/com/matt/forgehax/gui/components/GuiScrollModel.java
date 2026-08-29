package com.matt.forgehax.gui.components;

/** A small, non-animated scroll state for independently scrolling panes. */
public final class GuiScrollModel {

  private int offset;
  private int contentExtent;
  private int viewportExtent;

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
    clamp();
  }

  public int getContentExtent() {
    return contentExtent;
  }

  public int getViewportExtent() {
    return viewportExtent;
  }

  public int getMaxOffset() {
    return Math.max(0, contentExtent - viewportExtent);
  }

  public void setExtent(int contentExtent, int viewportExtent) {
    this.contentExtent = Math.max(0, contentExtent);
    this.viewportExtent = Math.max(0, viewportExtent);
    clamp();
  }

  public int scrollBy(int amount) {
    int previous = offset;
    offset += amount;
    clamp();
    return offset - previous;
  }

  public void scrollToTop() {
    offset = 0;
  }

  public void scrollToBottom() {
    offset = getMaxOffset();
  }

  public boolean canScroll() {
    return getMaxOffset() > 0;
  }

  private void clamp() {
    offset = Math.max(0, Math.min(getMaxOffset(), offset));
  }
}
