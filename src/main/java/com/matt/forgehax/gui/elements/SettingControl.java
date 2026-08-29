package com.matt.forgehax.gui.elements;

import com.matt.forgehax.util.command.Setting;

import java.util.*;

/**
 * Adapter used by the ClickGUI for one setting. It contains no rendering or GUI state; the screen
 * owns layout and focus while this class keeps all value changes on the existing Setting API.
 */
public final class SettingControl {

  private final Setting<?> setting;

  private SettingControl(Setting<?> setting) {
    if (setting == null) {
      throw new IllegalArgumentException("setting");
    }
    this.setting = setting;
  }

  /** Creates a control whose type is inferred from the setting converter. */
  public static SettingControl create(Setting<?> setting) {
    return new SettingControl(setting);
  }

  /** Creates deterministic controls for a collection of settings. */
  public static List<SettingControl> createAll(Collection<? extends Setting<?>> settings) {
    List<SettingControl> controls = new ArrayList<>();
    if (settings != null) {
      for (Setting<?> setting : settings) {
        if (setting != null) {
          controls.add(create(setting));
        }
      }
    }
    Collections.sort(
        controls,
        new Comparator<SettingControl>() {
          @Override
          public int compare(SettingControl left, SettingControl right) {
            int result =
                String.CASE_INSENSITIVE_ORDER.compare(
                    left.setting.getAbsoluteName(), right.setting.getAbsoluteName());
            return result != 0
                ? result
                : left.setting.getAbsoluteName().compareTo(right.setting.getAbsoluteName());
          }
        }
    );
    return Collections.unmodifiableList(controls);
  }

  private static Class<?> boxed(Class<?> type) {
    if (type == null || !type.isPrimitive()) {
      return type == null ? Object.class : type;
    }
    if (type == boolean.class) {
      return Boolean.class;
    }
    if (type == byte.class) {
      return Byte.class;
    }
    if (type == short.class) {
      return Short.class;
    }
    if (type == int.class) {
      return Integer.class;
    }
    if (type == long.class) {
      return Long.class;
    }
    if (type == float.class) {
      return Float.class;
    }
    if (type == double.class) {
      return Double.class;
    }
    if (type == char.class) {
      return Character.class;
    }
    return type;
  }

  private static double clamp(double value, double minimum, double maximum) {
    if (Double.isNaN(value)) {
      return minimum;
    }
    return Math.max(minimum, Math.min(maximum, value));
  }

  private static boolean isFinite(double value) {
    return !Double.isNaN(value) && !Double.isInfinite(value);
  }

  private static <T> String valueText(Setting<T> setting) {
    return setting.getConverter().toStringSafe(setting.get());
  }

  public Setting<?> getSetting() {
    return setting;
  }

  public String getName() {
    return setting.getName();
  }

  public String getAbsoluteName() {
    return setting.getAbsoluteName();
  }

  public String getDescription() {
    return setting.getDescription();
  }

  public Object getValue() {
    return setting.get();
  }

  public String getValueText() {
    return valueText(setting);
  }

  /** Returns a compact value for display without changing the exact editable value. */
  public String getDisplayValueText() {
    Class<?> type = boxed(getValueType());
    Object value = setting.get();
    if ((type == Float.class || type == Double.class) && value instanceof Number) {
      return String.format(Locale.ROOT, "%.1f", ((Number) value).doubleValue());
    }
    return getValueText();
  }

  public Object getDefaultValue() {
    return setting.getDefault();
  }

  public Class<?> getValueType() {
    Class<?> type = setting.getType();
    if (type != null) {
      return type;
    }
    if (setting.get() != null) {
      return setting.get().getClass();
    }
    return setting.getDefault() != null ? setting.getDefault().getClass() : Object.class;
  }

  public Type getType() {
    Class<?> type = boxed(getValueType());
    if (type == Boolean.class) {
      return Type.BOOLEAN;
    }
    if (type.isEnum()) {
      return Type.ENUM;
    }
    if (Number.class.isAssignableFrom(type)) {
      return Type.NUMBER;
    }
    return Type.TEXT;
  }

  public boolean isBoolean() {
    return getType() == Type.BOOLEAN;
  }

  public boolean isEnum() {
    return getType() == Type.ENUM;
  }

  public boolean isNumber() {
    return getType() == Type.NUMBER;
  }

  public boolean isText() {
    return getType() == Type.TEXT;
  }

  /** Returns true when this number has both bounds and can be represented by a slider. */
  public boolean hasSlider() {
    Number minimum = getMinimum();
    Number maximum = getMaximum();
    if (!isNumber() || minimum == null || maximum == null) {
      return false;
    }
    double min = minimum.doubleValue();
    double max = maximum.doubleValue();
    double range = max - min;
    return isFinite(min) && isFinite(max) && isFinite(range) && range > 0D;
  }

  public Number getMinimum() {
    Object minimum = setting.getMin();
    return minimum instanceof Number ? (Number) minimum : null;
  }

  public Number getMaximum() {
    Object maximum = setting.getMax();
    return maximum instanceof Number ? (Number) maximum : null;
  }

  /** Returns the bounded number's normalized slider position, or zero for non-slider controls. */
  public double getSliderFraction() {
    Number minimum = getMinimum();
    Number maximum = getMaximum();
    Object value = setting.get();
    if (!hasSlider() || !(value instanceof Number)) {
      return 0D;
    }
    double range = maximum.doubleValue() - minimum.doubleValue();
    double fraction = (((Number) value).doubleValue() - minimum.doubleValue()) / range;
    return clamp(fraction, 0D, 1D);
  }

  /** Applies a bounded numeric slider value and rereads the source after callbacks run. */
  public boolean setSliderFraction(double fraction) {
    Number minimum = getMinimum();
    Number maximum = getMaximum();
    if (!hasSlider()) {
      return false;
    }

    fraction = clamp(fraction, 0D, 1D);
    double value = minimum.doubleValue() + (maximum.doubleValue() - minimum.doubleValue()) * fraction;
    Object converted = convertSliderValue(value);
    return converted != null && commit(converted);
  }

  /** Applies a boolean setting through Setting.set so change callbacks remain authoritative. */
  public boolean setBoolean(boolean value) {
    return isBoolean() && commit(Boolean.valueOf(value));
  }

  /**
   * Applies an enum constant directly. This avoids the CLI converter's fuzzy matching and keeps
   * dropdown choices explicit.
   */
  public boolean setEnum(Enum<?> value) {
    Class<?> type = getValueType();
    return isEnum() && type.isInstance(value) && commit(value);
  }

  /** Returns a copy of the enum choices for an explicit dropdown. */
  public Enum<?>[] getEnumValues() {
    if (!isEnum()) {
      return new Enum<?>[0];
    }
    Object[] constants = getValueType().getEnumConstants();
    if (constants == null) {
      return new Enum<?>[0];
    }
    Enum<?>[] values = new Enum<?>[constants.length];
    System.arraycopy(constants, 0, values, 0, constants.length);
    return values;
  }

  /** Parses and applies text using the setting's converter. Invalid input never writes null. */
  public boolean setText(String value) {
    if (value == null) {
      return false;
    }
    Object parsed = setting.getConverter().parseSafe(value);
    return parsed != null && commit(parsed);
  }

  /** Resets through Setting.reset(), preserving callback cancellation and persistence semantics. */
  public boolean reset() {
    Object before = setting.get();
    try {
      setting.reset();
    } finally {
      Object after = setting.get();
      if (!Objects.equals(before, after)) {
        setting.serialize();
      }
    }
    return !Objects.equals(before, setting.get());
  }

  public boolean isModified() {
    return !Objects.equals(setting.get(), setting.getDefault());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private boolean commit(Object value) {
    Object before = setting.get();
    try {
      ((Setting) setting).set(value);
    } finally {
      // A change callback may cancel or clamp the request; the source value is authoritative.
      Object after = setting.get();
      if (!Objects.equals(before, after)) {
        setting.serialize();
      }
    }
    return !Objects.equals(before, setting.get());
  }

  private Object convertSliderValue(double value) {
    Class<?> type = boxed(getValueType());
    if (type == Byte.class) {
      return Byte.valueOf((byte) Math.round(value));
    }
    if (type == Short.class) {
      return Short.valueOf((short) Math.round(value));
    }
    if (type == Integer.class) {
      return Integer.valueOf((int) Math.round(value));
    }
    if (type == Long.class) {
      return Long.valueOf(Math.round(value));
    }
    if (type == Float.class) {
      return Float.valueOf((float) value);
    }
    if (type == Double.class) {
      return Double.valueOf(value);
    }
    return setting.getConverter().parseSafe(Double.toString(value));
  }

  public enum Type {
    BOOLEAN,
    ENUM,
    NUMBER,
    TEXT
  }
}
