package com.matt.forgehax.gui.components;

/**
 * Geometry for the fixed, centered ForgeHax modal.
 *
 * <p>All coordinates are in the scaled GUI coordinate space used by
 * {@code GuiScreen}.  The calculation is deliberately stateless so it can be
 * repeated from {@code initGui()} after a resolution or GUI-scale change.
 */
public final class ModalGeometry {

  public static final int MAX_WIDTH = 960;
  public static final int MAX_HEIGHT = 560;
  public static final int SCREEN_MARGIN = 12;
  public static final int HEADER_HEIGHT = 34;
  public static final int MIN_SIDEBAR_WIDTH = 120;
  public static final int MAX_SIDEBAR_WIDTH = 220;
  public static final float SIDEBAR_RATIO = 0.30F;

  private final int screenWidth;
  private final int screenHeight;
  private final GuiRect modal;
  private final GuiRect header;
  private final GuiRect sidebar;
  private final GuiRect content;
  private final GuiRect settings;

  private ModalGeometry(
      int screenWidth,
      int screenHeight,
      GuiRect modal,
      GuiRect header,
      GuiRect sidebar,
      GuiRect content,
      GuiRect settings
  ) {
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
    this.modal = modal;
    this.header = header;
    this.sidebar = sidebar;
    this.content = content;
    this.settings = settings;
  }

  /** Computes centered modal bounds for the current scaled screen. */
  public static ModalGeometry calculate(int screenWidth, int screenHeight) {
    int safeScreenWidth = Math.max(0, screenWidth);
    int safeScreenHeight = Math.max(0, screenHeight);
    int width = Math.min(MAX_WIDTH, Math.max(0, safeScreenWidth - SCREEN_MARGIN * 2));
    int height = Math.min(MAX_HEIGHT, Math.max(0, safeScreenHeight - SCREEN_MARGIN * 2));
    int x = (safeScreenWidth - width) / 2;
    int y = (safeScreenHeight - height) / 2;

    int headerHeight = Math.min(HEADER_HEIGHT, height);
    int sidebarWidth = Math.round(width * SIDEBAR_RATIO);
    sidebarWidth = Math.max(MIN_SIDEBAR_WIDTH, Math.min(MAX_SIDEBAR_WIDTH, sidebarWidth));
    // A very small window must not allow the sidebar to extend beyond the
    // modal.  Normal Minecraft resolutions are larger than the minimum.
    sidebarWidth = Math.min(width, sidebarWidth);

    GuiRect modal = new GuiRect(x, y, width, height);
    GuiRect header = new GuiRect(x, y, width, headerHeight);
    GuiRect content = new GuiRect(x, y + headerHeight, width, height - headerHeight);
    GuiRect sidebar = new GuiRect(content.getX(), content.getY(), sidebarWidth, content.getHeight());
    GuiRect settings = new GuiRect(
        content.getX() + sidebarWidth,
        content.getY(),
        content.getWidth() - sidebarWidth,
        content.getHeight()
    );
    return new ModalGeometry(safeScreenWidth, safeScreenHeight, modal, header, sidebar, content, settings);
  }

  public int getScreenWidth() {
    return screenWidth;
  }

  public int getScreenHeight() {
    return screenHeight;
  }

  public GuiRect getModal() {
    return modal;
  }

  public GuiRect getHeader() {
    return header;
  }

  public GuiRect getContent() {
    return content;
  }

  public GuiRect getSidebar() {
    return sidebar;
  }

  public GuiRect getSettings() {
    return settings;
  }

  public int getX() {
    return modal.getX();
  }

  public int getY() {
    return modal.getY();
  }

  public int getWidth() {
    return modal.getWidth();
  }

  public int getHeight() {
    return modal.getHeight();
  }

  public int getHeaderHeight() {
    return header.getHeight();
  }

  public int getSidebarWidth() {
    return sidebar.getWidth();
  }

  public boolean contains(int x, int y) {
    return modal.contains(x, y);
  }
}
