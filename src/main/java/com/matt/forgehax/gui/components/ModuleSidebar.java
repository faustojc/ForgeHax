package com.matt.forgehax.gui.components;

import com.matt.forgehax.Globals;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.mod.BaseMod;
import net.minecraft.client.gui.ScaledResolution;

import java.util.List;

/**
 * The module-only sidebar.  Rows select a module; they never toggle it.
 * Rendering intentionally stays flat and immediate so the parent screen can
 * place this view in the modal without introducing another state system.
 */
public final class ModuleSidebar implements Globals {

  public static final int ROW_HEIGHT = 26;
  public static final int ROW_PADDING = 10;
  public static final int STATUS_DOT_SIZE = 4;

  private final ModuleCatalog catalog;
  private final GuiScrollModel scroll = new GuiScrollModel();
  private GuiRect bounds = new GuiRect(0, 0, 0, 0);
  private BaseMod selected;

  public ModuleSidebar() {
    this(new ModuleCatalog());
  }

  public ModuleSidebar(ModuleCatalog catalog) {
    if (catalog == null) {
      throw new IllegalArgumentException("catalog");
    }
    this.catalog = catalog;
    List<BaseMod> modules = catalog.getModules();
    if (!modules.isEmpty()) {
      selected = modules.get(0);
    }
    updateScrollExtent();
  }

  private static BaseMod findByName(List<BaseMod> modules, String name) {
    if (name == null) {
      return null;
    }
    for (BaseMod module : modules) {
      if (name.equalsIgnoreCase(module.getModName())) {
        return module;
      }
    }
    return null;
  }

  public ModuleCatalog getCatalog() {
    return catalog;
  }

  public GuiRect getBounds() {
    return bounds;
  }

  public void setBounds(GuiRect bounds) {
    this.bounds = bounds == null ? new GuiRect(0, 0, 0, 0) : bounds;
    updateScrollExtent();
  }

  public void refresh() {
    String selectedName = selected == null ? null : selected.getModName();
    catalog.refresh();
    if (selectedName != null) {
      selected = findByName(catalog.getModules(), selectedName);
    }
    if (selected == null && !catalog.getModules().isEmpty()) {
      selected = catalog.getModules().get(0);
    }
    updateScrollExtent();
  }

  public String getQuery() {
    return catalog.getQuery();
  }

  public void setQuery(String query) {
    catalog.setQuery(query);
    updateScrollExtent();
  }

  public List<BaseMod> getVisibleModules() {
    return catalog.getFilteredModules();
  }

  public BaseMod getSelected() {
    return selected;
  }

  /** Selects a live module and leaves its enabled state untouched. */
  public boolean select(BaseMod mod) {
    if (mod == null || !catalog.getModules().contains(mod)) {
      return false;
    }
    selected = mod;
    return true;
  }

  /** Selects the module row under the pointer, if any. */
  public boolean selectAt(int mouseX, int mouseY) {
    BaseMod mod = getModuleAt(mouseX, mouseY);
    return select(mod);
  }

  /** Returns the row's module without changing selection or module state. */
  public BaseMod getModuleAt(int mouseX, int mouseY) {
    if (!bounds.contains(mouseX, mouseY)) {
      return null;
    }
    int row = (mouseY - bounds.getY() + scroll.getOffset()) / ROW_HEIGHT;
    List<BaseMod> visible = getVisibleModules();
    return row >= 0 && row < visible.size() ? visible.get(row) : null;
  }

  public GuiScrollModel getScroll() {
    return scroll;
  }

  public void scrollBy(int pixels) {
    scroll.scrollBy(pixels);
  }

  /** Scrolls by complete rows; positive values move down. */
  public void scrollRows(int rows) {
    scroll.scrollBy(rows * ROW_HEIGHT);
  }

  /** Draws the sidebar and its clipped rows using the current GUI scale. */
  public void draw(int mouseX, int mouseY) {
    SurfaceHelper.drawRect(
        bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), GuiPalette.MODAL);
    if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
      return;
    }
    updateScrollExtent();
    ScaledResolution resolution = new ScaledResolution(MC);
    try (GuiScissor.Clip ignored = GuiScissor.begin(bounds, resolution)) {
      List<BaseMod> visible = getVisibleModules();
      int firstY = bounds.getY() - scroll.getOffset();
      for (int i = 0; i < visible.size(); i++) {
        BaseMod mod = visible.get(i);
        int y = firstY + i * ROW_HEIGHT;
        boolean hovered = bounds.contains(mouseX, mouseY) && mouseY >= y && mouseY < y + ROW_HEIGHT;
        boolean active = mod == selected;
        if (active) {
          SurfaceHelper.drawRect(
              bounds.getX(), y, bounds.getWidth(), ROW_HEIGHT, GuiPalette.SURFACE);
          SurfaceHelper.drawRect(bounds.getX(), y, 2, ROW_HEIGHT, GuiPalette.ACCENT);
        } else if (hovered) {
          SurfaceHelper.drawRect(
              bounds.getX(), y, bounds.getWidth(), ROW_HEIGHT, GuiPalette.HOVER);
        }
        int dotColor = mod.isEnabled() ? GuiPalette.ACCENT : GuiPalette.DIVIDER;
        SurfaceHelper.drawRect(
            bounds.getX() + ROW_PADDING,
            y + (ROW_HEIGHT - STATUS_DOT_SIZE) / 2,
            STATUS_DOT_SIZE,
            STATUS_DOT_SIZE,
            dotColor
        );
        SurfaceHelper.drawText(
            mod.getModName(),
            bounds.getX() + ROW_PADDING * 2,
            y + (ROW_HEIGHT - SurfaceHelper.getTextHeight()) / 2,
            active ? GuiPalette.TEXT : GuiPalette.TEXT_MUTED
        );
      }
      if (visible.isEmpty()) {
        SurfaceHelper.drawText(
            "No modules match your search.",
            bounds.getX() + ROW_PADDING,
            bounds.getY() + ROW_PADDING,
            GuiPalette.TEXT_MUTED
        );
      }
    }
  }

  private void updateScrollExtent() {
    scroll.setExtent(getVisibleModules().size() * ROW_HEIGHT, bounds.getHeight());
  }
}
