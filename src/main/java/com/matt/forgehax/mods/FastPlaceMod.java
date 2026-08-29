package com.matt.forgehax.mods;

import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created on 9/4/2016 by fr1kin
 */
@RegisterMod
public class FastPlaceMod extends ToggleMod {

  private final Setting<Integer> ticks =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("ticks")
          .description("Ticks between placements, 1 is every tick and 4 is vanilla speed")
          .defaultTo(1)
          .min(1)
          .max(4)
          .build();

  public FastPlaceMod() {
    super(Category.PLAYER, "FastPlace", false, "Fast place");
  }

  @SubscribeEvent
  public void onUpdate(LocalPlayerUpdateEvent event) {
    // vanilla sets the timer to 4 after a placement, only ever lower it so the setting caps the rate
    final int delay = ticks.get() - 1;
    if (FastReflection.Fields.Minecraft_rightClickDelayTimer.get(MC) > delay) {
      FastReflection.Fields.Minecraft_rightClickDelayTimer.set(MC, delay);
    }
  }
}
