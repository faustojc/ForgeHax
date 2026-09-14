package com.matt.forgehax.mods;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;

@RegisterMod
public class FullBrightMod extends ToggleMod {

  private final Setting<Double> defaultGamma =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("gamma")
          .description("default gamma to revert to")
          .defaultTo(MC.options.gamma().get())
          .min(0.1D)
          .max(16D)
          .build();

  public FullBrightMod() {
    super(Category.WORLD, "FullBright", false, "Makes everything render with maximum brightness");
  }

  @Override
  public void onEnabled() {
    MC.options.gamma().set(16D);
  }

  @Override
  public void onDisabled() {
    MC.options.gamma().set(defaultGamma.get());
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    MC.options.gamma().set(16D);
  }
}
