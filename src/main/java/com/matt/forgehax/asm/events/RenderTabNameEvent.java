package com.matt.forgehax.asm.events;

import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;
import java.util.OptionalInt;

public class RenderTabNameEvent extends Event {

  private final String name;
  private final int color;

  private @Nullable
  String newName;
  private OptionalInt newColor = OptionalInt.empty();

  public RenderTabNameEvent(String name, int color) {
    this.name = name;
    this.color = color;
  }

  public String getName() {
    return this.newName != null ? this.newName : this.name;
  }

  public void setName(String newName) {
    this.newName = newName;
  }

  public int getColor() {
    return newColor.orElse(this.color);
  }

  public void setColor(int newColor) {
    this.newColor = OptionalInt.of(newColor);
  }
}
