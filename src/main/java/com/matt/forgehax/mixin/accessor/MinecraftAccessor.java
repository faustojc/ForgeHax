package com.matt.forgehax.mixin.accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Timer;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Private client state the 1.12.2 build reached through reflection.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {

  @Accessor("missTime")
  int getMissTime();

  @Accessor("missTime")
  void setMissTime(int value);

  @Accessor("rightClickDelay")
  int getRightClickDelay();

  @Accessor("rightClickDelay")
  void setRightClickDelay(int value);

  @Accessor("timer")
  Timer getTimer();

  @Accessor("user")
  @Mutable
  void setUser(User user);
}
