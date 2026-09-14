package com.matt.forgehax.gui;

import com.matt.forgehax.Globals;
import com.matt.forgehax.gui.components.*;
import com.matt.forgehax.gui.elements.KeybindControl;
import com.matt.forgehax.gui.elements.SettingControl;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.CommandStub;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.BaseMod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Large, non-animated module control center. */
public class ClickGui extends Screen implements Globals {

  private static final int SIDEBAR_TOP = 36;
  private static final int MODULE_HEADER_H = 78;
  private static final int ROW_H = 46;
  private static final int CONTROL_H = 18;
  private static final int ERROR = 0xFFE05A5A;
  private static ClickGui INSTANCE;

  private final ModuleSidebar sidebar = new ModuleSidebar();
  private final GuiScrollModel settingsScroll = new GuiScrollModel();
  private final StringBuilder search = new StringBuilder();
  private final StringBuilder editor = new StringBuilder();
  private ModalGeometry geometry;
  private GuiRect searchBox = new GuiRect(0, 0, 0, 0);
  private GuiRect moduleList = new GuiRect(0, 0, 0, 0);
  private GuiRect moduleHeader = new GuiRect(0, 0, 0, 0);
  private GuiRect settingsView = new GuiRect(0, 0, 0, 0);
  private GuiRect closeBox = new GuiRect(0, 0, 0, 0);
  private BaseMod selected;
  private List<SettingControl> controls = Collections.emptyList();
  private KeybindControl keybind;
  private boolean cliOnly;
  private boolean searchFocused;
  private int searchCursor;
  private SettingControl editing;
  private int editorCursor;
  private boolean editorError;
  private SettingControl expandedEnum;
  private SettingControl draggingSlider;
  private boolean capturingBind;
  private GuiGraphics graphics;

  private ClickGui() {
    super(Component.literal("ForgeHax"));
  }

  public static ClickGui getInstance() {
    return INSTANCE == null ? (INSTANCE = new ClickGui()) : INSTANCE;
  }

  public boolean isCapturingInput() {
    return searchFocused || editing != null || expandedEnum != null || capturingBind;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    this.graphics = graphics;
    if (geometry == null || geometry.getScreenWidth() != width || geometry.getScreenHeight() != height) {
      layout();
    }
    if (draggingSlider != null) {
      if (GLFW.glfwGetMouseButton(MC.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
          == GLFW.GLFW_PRESS) {
        setSliderFromMouse(draggingSlider, mouseX);
      } else {
        draggingSlider = null;
      }
    }
    drawRect(0, 0, width, height, GuiPalette.BACKDROP);
    GuiRect modal = geometry.getModal();
    drawRect(modal.getX(), modal.getY(), modal.getWidth(), modal.getHeight(), GuiPalette.MODAL);
    border(modal, GuiPalette.DIVIDER);
    drawTopBar(mouseX, mouseY);
    drawSearch();
    sidebar.draw(graphics, mouseX, mouseY);
    drawModuleHeader(mouseX, mouseY);
    drawSettings(graphics, mouseX, mouseY);
    drawEnumMenu(graphics, mouseX, mouseY);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (capturingBind) {
      keybind.captureKeyboard(keyCode);
      capturingBind = false;
      return true;
    }
    if (expandedEnum != null) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
        expandedEnum = null;
      }
      return true;
    }
    if (editing != null) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
        editing = null;
        editor.setLength(0);
        editorError = false;
      } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
        commitEditor();
      } else {
        editorCursor = editBuffer(editor, editorCursor, '\0', keyCode);
        editorError = false;
      }
      return true;
    }
    if (searchFocused) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
        if (search.length() > 0) {
          search.setLength(0);
          searchCursor = 0;
          applySearch();
        } else {
          searchFocused = false;
        }
      } else {
        searchCursor = editBuffer(search, searchCursor, '\0', keyCode);
        applySearch();
      }
      return true;
    }
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      MC.setScreen(null);
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char typedChar, int modifiers) {
    if (editing != null) {
      if (SharedConstants.isAllowedChatCharacter(typedChar)) {
        editorCursor = editBuffer(editor, editorCursor, typedChar, -1);
        editorError = false;
      }
      return true;
    }
    if (searchFocused) {
      if (SharedConstants.isAllowedChatCharacter(typedChar)) {
        searchCursor = editBuffer(search, searchCursor, typedChar, -1);
        applySearch();
      }
      return true;
    }
    return super.charTyped(typedChar, modifiers);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
    int x = (int) mouseX;
    int y = (int) mouseY;
    if (capturingBind) {
      keybind.captureMouse(mouseButton);
      capturingBind = false;
      return true;
    }
    if (closeBox.contains(x, y)) {
      MC.setScreen(null);
      return true;
    }
    if (expandedEnum != null) {
      GuiRect menu = enumMenuBox(expandedEnum);
      if (menu.contains(x, y)) {
        int option = (y - menu.getY()) / CONTROL_H;
        Enum<?>[] values = expandedEnum.getEnumValues();
        if (option >= 0 && option < values.length) {
          expandedEnum.setEnum(values[option]);
        }
        expandedEnum = null;
        return true;
      }
      expandedEnum = null;
    }
    if (searchBox.contains(x, y)) {
      if (editing != null && !commitEditor()) {
        return true;
      }
      searchFocused = true;
      searchCursor = search.length();
      return true;
    }
    searchFocused = false;
    if (!geometry.contains(x, y)) {
      return true;
    }
    if (editing != null) {
      if (editableBox(editing).contains(x, y)) {
        return true;
      }
      if (!commitEditor()) {
        return true;
      }
    }
    if (moduleList.contains(x, y)) {
      BaseMod before = selected;
      sidebar.selectAt(x, y);
      selected = sidebar.getSelected();
      if (before != selected) {
        rebuildControls();
      }
      return true;
    }
    if (selected == null) {
      return true;
    }
    if (toggleBox().contains(x, y)) {
      if (selected.isEnabled()) {
        selected.disable();
      } else {
        selected.enable();
      }
      Setting<?> enabled = selected.getSetting("enabled");
      if (enabled != null) {
        enabled.serialize();
      }
      return true;
    }
    if (keybind != null && keybind.isAvailable() && bindBox().contains(x, y)) {
      clearEditors();
      capturingBind = true;
      return true;
    }
    SettingControl control = controlAt(y);
    if (control == null) {
      return true;
    }
    int rowY = rowY(control);
    if (control.isModified() && resetBox(rowY).contains(x, y)) {
      control.reset();
      return true;
    }
    GuiRect rect = controlBox(rowY);
    if (control.isBoolean() && rect.contains(x, y)) {
      control.setBoolean(!Boolean.TRUE.equals(control.getValue()));
    } else if (control.isEnum() && rect.contains(x, y)) {
      expandedEnum = control;
    } else if (control.isNumber() && control.hasSlider() && sliderBox(rowY).contains(x, y)) {
      draggingSlider = control;
      setSliderFromMouse(control, x);
    } else if (editableBox(control).contains(x, y)) {
      beginEditor(control);
    }
    return true;
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int state) {
    draggingSlider = null;
    return super.mouseReleased(mouseX, mouseY, state);
  }

  @Override
  protected void init() {
    super.init();
    search.setLength(0);
    searchCursor = 0;
    searchFocused = false;
    clearEditors();
    layout();
    sidebar.refresh();
    sidebar.setQuery("");
    if (selected != null) {
      sidebar.select(selected);
    }
    selected = sidebar.getSelected();
    rebuildControls();
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta == 0) {
      return super.mouseScrolled(mouseX, mouseY, delta);
    }
    int direction = delta < 0 ? 1 : -1;
    int x = (int) mouseX;
    int y = (int) mouseY;
    if (moduleList.contains(x, y)) {
      sidebar.scrollRows(direction * 3);
    } else if (settingsView.contains(x, y)) {
      settingsScroll.scrollBy(direction * ROW_H * 2);
      expandedEnum = null;
    }
    return true;
  }

  @Override
  public void onClose() {
    commitEditor();
    clearEditors();
    searchFocused = false;
    super.onClose();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  private void layout() {
    geometry = ModalGeometry.calculate(width, height);
    GuiRect left = geometry.getSidebar();
    searchBox = new GuiRect(left.getX() + 8, left.getY() + 8, Math.max(0, left.getWidth() - 16), 20);
    moduleList = new GuiRect(
        left.getX(), left.getY() + SIDEBAR_TOP, left.getWidth(), Math.max(0, left.getHeight() - SIDEBAR_TOP));
    sidebar.setBounds(moduleList);
    GuiRect right = geometry.getSettings();
    moduleHeader = new GuiRect(right.getX(), right.getY(), right.getWidth(), MODULE_HEADER_H);
    settingsView = new GuiRect(
        right.getX(), right.getY() + MODULE_HEADER_H, right.getWidth(), Math.max(0, right.getHeight() - MODULE_HEADER_H));
    closeBox = new GuiRect(geometry.getHeader().getRight() - 30, geometry.getHeader().getY() + 5, 24, 24);
    updateSettingsExtent();
  }

  private void drawTopBar(int mouseX, int mouseY) {
    GuiRect header = geometry.getHeader();
    drawRect(header.getX(), header.getY(), header.getWidth(), header.getHeight(), GuiPalette.SURFACE);
    drawRect(header.getX(), header.getBottom() - 1, header.getWidth(), 1, GuiPalette.DIVIDER);
    drawText("ForgeHax", header.getX() + 12, header.getY() + 12, GuiPalette.TEXT);
    if (closeBox.contains(mouseX, mouseY)) {
      drawRect(closeBox.getX(), closeBox.getY(), closeBox.getWidth(), closeBox.getHeight(), GuiPalette.HOVER);
    }
    drawText(
        "x", closeBox.getX() + (closeBox.getWidth() - textWidth("x")) / 2,
        closeBox.getY() + 8, GuiPalette.TEXT_MUTED
    );
  }

  private void drawSearch() {
    drawRect(searchBox.getX(), searchBox.getY(), searchBox.getWidth(), searchBox.getHeight(), GuiPalette.SURFACE);
    border(searchBox, searchFocused ? GuiPalette.ACCENT : GuiPalette.DIVIDER);
    String value = search.length() == 0 && !searchFocused ? "Search modules..." : search.toString();
    if (searchFocused) {
      value = value.substring(0, searchCursor) + "|" + value.substring(searchCursor);
    }
    drawText(
        fitEnd(value, searchBox.getWidth() - 12), searchBox.getX() + 6,
        searchBox.getY() + 7, search.length() == 0 ? GuiPalette.TEXT_MUTED : GuiPalette.TEXT
    );
  }

  private void drawModuleHeader(int mouseX, int mouseY) {
    drawRect(moduleHeader.getX(), moduleHeader.getY(), moduleHeader.getWidth(), moduleHeader.getHeight(), GuiPalette.MODAL);
    drawRect(moduleHeader.getX(), moduleHeader.getBottom() - 1, moduleHeader.getWidth(), 1, GuiPalette.DIVIDER);
    if (selected == null) {
      drawText("Select a module", moduleHeader.getX() + 12, moduleHeader.getY() + 14, GuiPalette.TEXT_MUTED);
      return;
    }
    GuiRect toggle = toggleBox();
    drawText(
        fit(selected.getModName(), Math.max(0, toggle.getX() - moduleHeader.getX() - 24)),
        moduleHeader.getX() + 12, moduleHeader.getY() + 13, GuiPalette.TEXT
    );
    drawText(
        fit(selected.getModDescription(), Math.max(0, moduleHeader.getWidth() - 148)),
        moduleHeader.getX() + 12, moduleHeader.getY() + 31, GuiPalette.TEXT_MUTED
    );
    drawRect(
        toggle.getX(), toggle.getY(), toggle.getWidth(), toggle.getHeight(),
        selected.isEnabled() ? GuiPalette.ACCENT : GuiPalette.SURFACE
    );
    border(toggle, selected.isEnabled() ? GuiPalette.ACCENT : GuiPalette.DIVIDER);
    drawText(selected.isEnabled() ? "ON" : "OFF", toggle.getX() + 15, toggle.getY() + 7, GuiPalette.TEXT);

    drawText("Keybind", moduleHeader.getX() + 12, moduleHeader.getY() + 56, GuiPalette.TEXT_MUTED);
    if (keybind != null && keybind.isAvailable()) {
      GuiRect bind = bindBox();
      drawRect(bind.getX(), bind.getY(), bind.getWidth(), bind.getHeight(), GuiPalette.SURFACE);
      border(bind, capturingBind ? GuiPalette.ACCENT : GuiPalette.DIVIDER);
      String label = capturingBind ? "Press a key..." : keybind.getDisplayName();
      drawText(fit(label, bind.getWidth() - 10), bind.getX() + 5, bind.getY() + 7, GuiPalette.TEXT);
    } else {
      drawText("Unavailable", moduleHeader.getX() + 64, moduleHeader.getY() + 56, GuiPalette.TEXT_MUTED);
    }
  }

  private void drawSettings(GuiGraphics graphics, int mouseX, int mouseY) {
    drawRect(settingsView.getX(), settingsView.getY(), settingsView.getWidth(), settingsView.getHeight(), GuiPalette.MODAL);
    updateSettingsExtent();
    if (settingsView.getWidth() <= 0 || settingsView.getHeight() <= 0) {
      return;
    }
    try (GuiScissor.Clip ignored = GuiScissor.begin(graphics, settingsView)) {
      int firstY = settingsView.getY() - settingsScroll.getOffset();
      for (int i = 0; i < controls.size(); i++) {
        drawSettingRow(controls.get(i), firstY + i * ROW_H, mouseX, mouseY);
      }
      int footerY = firstY + controls.size() * ROW_H + 12;
      if (controls.isEmpty() && selected != null) {
        drawText("No editable settings", settingsView.getX() + 12, footerY, GuiPalette.TEXT_MUTED);
      }
      if (cliOnly) {
        drawText(
            "Additional actions remain available in the CLI.", settingsView.getX() + 12,
            footerY + (controls.isEmpty() ? 18 : 0), GuiPalette.TEXT_MUTED
        );
      }
    }
  }

  private void drawSettingRow(SettingControl control, int rowY, int mouseX, int mouseY) {
    if (rowY + ROW_H <= settingsView.getY() || rowY >= settingsView.getBottom()) {
      return;
    }
    if (mouseX >= settingsView.getX() && mouseX < settingsView.getRight() && mouseY >= rowY && mouseY < rowY + ROW_H) {
      drawRect(settingsView.getX(), rowY, settingsView.getWidth(), ROW_H, GuiPalette.HOVER);
    }
    drawRect(settingsView.getX() + 10, rowY + ROW_H - 1, Math.max(0, settingsView.getWidth() - 20), 1, GuiPalette.DIVIDER);
    GuiRect controlRect = controlBox(rowY);
    int labelWidth = Math.max(0, resetBox(rowY).getX() - settingsView.getX() - 20);
    drawText(fit(settingLabel(control), labelWidth), settingsView.getX() + 12, rowY + 9, GuiPalette.TEXT);
    drawText(fit(control.getDescription(), labelWidth), settingsView.getX() + 12, rowY + 25, GuiPalette.TEXT_MUTED);
    if (control.isModified()) {
      GuiRect reset = resetBox(rowY);
      drawText("R", reset.getX() + 5, reset.getY() + 5, GuiPalette.ACCENT);
    }
    if (control.isBoolean()) {
      drawBoolean(control, controlRect);
    } else if (control.isEnum()) {
      drawEnum(control, controlRect);
    } else if (control.isNumber()) {
      drawNumber(control, controlRect, rowY);
    } else {
      drawTextField(control, controlRect);
    }
  }

  private void drawBoolean(SettingControl control, GuiRect rect) {
    rect = new GuiRect(rect.getRight() - 38, rect.getY(), 38, rect.getHeight());
    boolean active = Boolean.TRUE.equals(control.getValue());
    drawRect(
        rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(),
        active ? GuiPalette.ACCENT : GuiPalette.SURFACE
    );
    border(rect, active ? GuiPalette.ACCENT : GuiPalette.DIVIDER);
    int knob = Math.max(6, rect.getHeight() - 6);
    int knobX = active ? rect.getRight() - knob - 3 : rect.getX() + 3;
    drawRect(knobX, rect.getY() + 3, knob, knob, GuiPalette.TEXT);
  }

  private void drawEnum(SettingControl control, GuiRect rect) {
    drawRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), GuiPalette.SURFACE);
    border(rect, expandedEnum == control ? GuiPalette.ACCENT : GuiPalette.DIVIDER);
    drawText(fit(control.getValueText(), rect.getWidth() - 20), rect.getX() + 5, rect.getY() + 6, GuiPalette.TEXT);
    drawText("v", rect.getRight() - 12, rect.getY() + 6, GuiPalette.TEXT_MUTED);
  }

  private void drawNumber(SettingControl control, GuiRect rect, int rowY) {
    if (!control.hasSlider()) {
      drawTextField(control, rect);
      return;
    }
    GuiRect slider = sliderBox(rowY);
    GuiRect value = numberBox(rowY);
    drawRect(slider.getX(), slider.getY() + slider.getHeight() / 2 - 1, slider.getWidth(), 2, GuiPalette.DIVIDER);
    int fill = (int) Math.round(slider.getWidth() * control.getSliderFraction());
    drawRect(slider.getX(), slider.getY() + slider.getHeight() / 2 - 1, fill, 2, GuiPalette.ACCENT);
    int knobX = Math.max(slider.getX(), Math.min(slider.getRight() - 4, slider.getX() + fill - 2));
    drawRect(knobX, slider.getY() + 4, 4, Math.max(4, slider.getHeight() - 8), GuiPalette.ACCENT);
    drawTextField(control, value);
  }

  private void drawTextField(SettingControl control, GuiRect rect) {
    drawRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), GuiPalette.SURFACE);
    int color = editing == control ? (editorError ? ERROR : GuiPalette.ACCENT) : GuiPalette.DIVIDER;
    border(rect, color);
    String value = control.getDisplayValueText();
    if (editing == control) {
      value = editor.substring(0, editorCursor) + "|" + editor.substring(editorCursor);
    }
    drawText(
        fitEnd(value, rect.getWidth() - 10), rect.getX() + 5, rect.getY() + 6,
        editorError && editing == control ? ERROR : GuiPalette.TEXT
    );
  }

  private void drawEnumMenu(GuiGraphics graphics, int mouseX, int mouseY) {
    if (expandedEnum == null) {
      return;
    }
    GuiRect menu = enumMenuBox(expandedEnum);
    Enum<?>[] values = expandedEnum.getEnumValues();
    if (menu.getWidth() <= 0 || menu.getHeight() <= 0) {
      return;
    }
    try (GuiScissor.Clip ignored = GuiScissor.begin(graphics, settingsView)) {
      drawRect(menu.getX(), menu.getY(), menu.getWidth(), menu.getHeight(), GuiPalette.SURFACE);
      border(menu, GuiPalette.DIVIDER);
      for (int i = 0; i < values.length; i++) {
        int y = menu.getY() + i * CONTROL_H;
        if (mouseX >= menu.getX() && mouseX < menu.getRight() && mouseY >= y && mouseY < y + CONTROL_H) {
          drawRect(menu.getX() + 1, y, Math.max(0, menu.getWidth() - 2), CONTROL_H, GuiPalette.HOVER);
        }
        drawText(
            fit(values[i].name(), menu.getWidth() - 10), menu.getX() + 5, y + 6,
            values[i].equals(expandedEnum.getValue()) ? GuiPalette.ACCENT : GuiPalette.TEXT
        );
      }
    }
  }

  private int editBuffer(StringBuilder buffer, int cursor, char typedChar, int keyCode) {
    if (keyCode == GLFW.GLFW_KEY_LEFT) {
      return Math.max(0, cursor - 1);
    }
    if (keyCode == GLFW.GLFW_KEY_RIGHT) {
      return Math.min(buffer.length(), cursor + 1);
    }
    if (keyCode == GLFW.GLFW_KEY_HOME) {
      return 0;
    }
    if (keyCode == GLFW.GLFW_KEY_END) {
      return buffer.length();
    }
    if (keyCode == GLFW.GLFW_KEY_BACKSPACE && cursor > 0) {
      buffer.deleteCharAt(cursor - 1);
      return cursor - 1;
    }
    if (keyCode == GLFW.GLFW_KEY_DELETE && cursor < buffer.length()) {
      buffer.deleteCharAt(cursor);
      return cursor;
    }
    if (hasControlDown() && keyCode == GLFW.GLFW_KEY_V) {
      String clipboard = MC.keyboardHandler.getClipboard();
      if (clipboard != null) {
        for (int i = 0; i < clipboard.length(); i++) {
          char value = clipboard.charAt(i);
          if (SharedConstants.isAllowedChatCharacter(value)) {
            buffer.insert(cursor++, value);
          }
        }
      }
      return cursor;
    }
    if (SharedConstants.isAllowedChatCharacter(typedChar)) {
      buffer.insert(cursor, typedChar);
      return cursor + 1;
    }
    return cursor;
  }

  private void rebuildControls() {
    clearEditors();
    settingsScroll.scrollToTop();
    cliOnly = false;
    if (selected == null || selected.getCommandStub() == null) {
      controls = Collections.emptyList();
      keybind = null;
      updateSettingsExtent();
      return;
    }
    Setting<?> enabled = selected.getSetting("enabled");
    List<Setting<?>> settings = new ArrayList<>();
    for (Command command : selected.getCommandStub().getChildrenDeep()) {
      if (command instanceof Setting) {
        if (command != enabled) {
          settings.add((Setting<?>) command);
        }
      } else {
        cliOnly = true;
      }
    }
    controls = SettingControl.createAll(settings);
    keybind = selected.getCommandStub() instanceof CommandStub
        ? new KeybindControl((CommandStub) selected.getCommandStub()) : null;
    updateSettingsExtent();
  }

  private void applySearch() {
    sidebar.setQuery(search.toString());
    List<BaseMod> visible = sidebar.getVisibleModules();
    if (selected == null || !visible.contains(selected)) {
      BaseMod next = visible.isEmpty() ? null : visible.get(0);
      if (next != null) {
        sidebar.select(next);
      }
      selected = next;
      rebuildControls();
    }
  }

  private void beginEditor(SettingControl control) {
    editing = control;
    editor.setLength(0);
    editor.append(control.getValueText());
    editorCursor = editor.length();
    editorError = false;
    expandedEnum = null;
  }

  private boolean commitEditor() {
    if (editing == null) {
      return true;
    }
    Object parsed = editing.getSetting().getConverter().parseSafe(editor.toString());
    if (parsed == null) {
      editorError = true;
      return false;
    }
    editing.setText(editor.toString());
    editing = null;
    editor.setLength(0);
    editorCursor = 0;
    editorError = false;
    return true;
  }

  private void clearEditors() {
    editing = null;
    editor.setLength(0);
    editorCursor = 0;
    editorError = false;
    expandedEnum = null;
    draggingSlider = null;
    capturingBind = false;
  }

  private void setSliderFromMouse(SettingControl control, int mouseX) {
    int rowY = rowY(control);
    GuiRect slider = sliderBox(rowY);
    if (slider.getWidth() > 0) {
      control.setSliderFraction((mouseX - slider.getX()) / (double) slider.getWidth());
    }
  }

  private SettingControl controlAt(int mouseY) {
    if (mouseY < settingsView.getY() || mouseY >= settingsView.getBottom()) {
      return null;
    }
    int index = (mouseY - settingsView.getY() + settingsScroll.getOffset()) / ROW_H;
    return index >= 0 && index < controls.size() ? controls.get(index) : null;
  }

  private int rowY(SettingControl control) {
    int index = controls.indexOf(control);
    return settingsView.getY() - settingsScroll.getOffset() + Math.max(0, index) * ROW_H;
  }

  private void updateSettingsExtent() {
    int footer = cliOnly || controls.isEmpty() ? 40 : 12;
    settingsScroll.setExtent(controls.size() * ROW_H + footer, settingsView.getHeight());
  }

  private GuiRect toggleBox() {
    return new GuiRect(moduleHeader.getRight() - 62, moduleHeader.getY() + 9, 48, 20);
  }

  private GuiRect bindBox() {
    return new GuiRect(moduleHeader.getRight() - 124, moduleHeader.getY() + 49, 110, 20);
  }

  private GuiRect controlBox(int rowY) {
    int width = Math.min(116, Math.max(70, settingsView.getWidth() / 3));
    return new GuiRect(settingsView.getRight() - width - 10, rowY + 12, width, CONTROL_H);
  }

  private GuiRect resetBox(int rowY) {
    GuiRect control = controlBox(rowY);
    return new GuiRect(control.getX() - 20, rowY + 13, 16, 16);
  }

  private GuiRect numberBox(int rowY) {
    GuiRect control = controlBox(rowY);
    int width = Math.min(52, Math.max(36, control.getWidth() / 2));
    return new GuiRect(control.getRight() - width, control.getY(), width, control.getHeight());
  }

  private GuiRect sliderBox(int rowY) {
    GuiRect control = controlBox(rowY);
    GuiRect number = numberBox(rowY);
    return new GuiRect(control.getX(), control.getY(), Math.max(0, number.getX() - control.getX() - 8), control.getHeight());
  }

  private GuiRect editableBox(SettingControl control) {
    int rowY = rowY(control);
    return control.isNumber() && control.hasSlider() ? numberBox(rowY) : controlBox(rowY);
  }

  private GuiRect enumMenuBox(SettingControl control) {
    GuiRect anchor = controlBox(rowY(control));
    int height = control.getEnumValues().length * CONTROL_H;
    int below = anchor.getBottom();
    int y = below + height <= settingsView.getBottom() ? below : anchor.getY() - height;
    return new GuiRect(anchor.getX(), y, anchor.getWidth(), height);
  }

  private String settingLabel(SettingControl control) {
    String name = control.getAbsoluteName();
    if (selected != null) {
      String prefix = selected.getModName() + ".";
      if (name.regionMatches(true, 0, prefix, 0, prefix.length())) {
        return name.substring(prefix.length());
      }
    }
    return name;
  }

  private String fit(String value, int width) {
    String safe = value == null ? "" : value;
    return width <= 0 ? "" : MC.font.plainSubstrByWidth(safe, width);
  }

  private String fitEnd(String value, int width) {
    String safe = value == null ? "" : value;
    if (width <= 0) {
      return "";
    }
    while (safe.length() > 0 && textWidth(safe) > width) {
      safe = safe.substring(1);
    }
    return safe;
  }

  private void drawRect(int x, int y, int width, int height, int color) {
    if (graphics != null && width > 0 && height > 0) {
      graphics.fill(x, y, x + width, y + height, color);
    }
  }

  private void drawText(String value, int x, int y, int color) {
    if (graphics != null) {
      graphics.drawString(MC.font, value, x, y, color);
    }
  }

  private int textWidth(String value) {
    return MC.font.width(value);
  }

  private void border(GuiRect rect, int color) {
    if (rect.getWidth() <= 0 || rect.getHeight() <= 0) {
      return;
    }
    drawRect(rect.getX(), rect.getY(), rect.getWidth(), 1, color);
    drawRect(rect.getX(), rect.getBottom() - 1, rect.getWidth(), 1, color);
    drawRect(rect.getX(), rect.getY(), 1, rect.getHeight(), color);
    drawRect(rect.getRight() - 1, rect.getY(), 1, rect.getHeight(), color);
  }
}
