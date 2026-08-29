package com.matt.forgehax.gui.components;

import com.matt.forgehax.Helper;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.BaseMod;

import java.util.*;

/**
 * Discovers and searches visible modules without using category metadata.
 * This is the single source for the sidebar's module list.
 */
public final class ModuleCatalog {

  private static final Comparator<BaseMod> MODULE_COMPARATOR = new Comparator<BaseMod>() {
    @Override
    public int compare(BaseMod left, BaseMod right) {
      int result = safe(left.getModName()).compareToIgnoreCase(safe(right.getModName()));
      if (result != 0) {
        return result;
      }
      return safe(left.getModName()).compareTo(safe(right.getModName()));
    }
  };

  private List<BaseMod> modules = Collections.emptyList();
  private String query = "";

  public ModuleCatalog() {
    refresh();
  }

  private static boolean contains(String value, String needle) {
    return value != null && normalize(value).contains(needle);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }

  /** Rebuilds the list from the live ModManager, preserving no copied state. */
  public void refresh() {
    List<BaseMod> discovered = new ArrayList<>();
    for (BaseMod mod : Helper.getModManager().getMods()) {
      if (mod != null && !mod.isHidden()) {
        discovered.add(mod);
      }
    }
    Collections.sort(discovered, MODULE_COMPARATOR);
    modules = Collections.unmodifiableList(discovered);
  }

  public List<BaseMod> getModules() {
    return modules;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query == null ? "" : query;
  }

  /** Returns a freshly filtered, sorted view of the visible module list. */
  public List<BaseMod> getFilteredModules() {
    String needle = normalize(query);
    if (needle.isEmpty()) {
      return modules;
    }
    List<BaseMod> filtered = new ArrayList<>();
    for (BaseMod mod : modules) {
      if (matches(mod, needle)) {
        filtered.add(mod);
      }
    }
    return Collections.unmodifiableList(filtered);
  }

  public boolean matches(BaseMod mod, String query) {
    if (mod == null) {
      return false;
    }
    String needle = normalize(query);
    if (needle.isEmpty()) {
      return true;
    }
    if (contains(mod.getModName(), needle) || contains(mod.getModDescription(), needle)) {
      return true;
    }
    Command root = mod.getCommandStub();
    if (root == null) {
      return false;
    }
    for (Command command : root.getChildrenDeep()) {
      if (command instanceof Setting
          && (contains(command.getName(), needle) || contains(command.getDescription(), needle))) {
        return true;
      }
    }
    return false;
  }
}
