package com.matt.forgehax.gui.components;

/** Immutable rectangle in scaled Minecraft GUI coordinates. */
public final class GuiRect {

  private final int x;
  private final int y;
  private final int width;
  private final int height;

  public GuiRect(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = Math.max(0, width);
    this.height = Math.max(0, height);
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getRight() {
    return x + width;
  }

  public int getBottom() {
    return y + height;
  }

  public boolean contains(int pointX, int pointY) {
    return pointX >= x
        && pointX < getRight()
        && pointY >= y
        && pointY < getBottom();
  }

  public GuiRect inset(int amount) {
    int inset = Math.max(0, amount);
    return new GuiRect(x + inset, y + inset, width - inset * 2, height - inset * 2);
  }

  @Override
  public String toString() {
    return "GuiRect{" + x + "," + y + " " + width + "x" + height + "}";
  }
}
