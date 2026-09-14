package com.matt.forgehax.mods;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.matt.forgehax.Helper.getRidingOrPlayer;

@RegisterMod
public class NoclipMod extends ToggleMod {

  public NoclipMod() {
    super(Category.PLAYER, "Noclip", false, "Enables player noclip");
  }

  @Override
  public void onDisabled() {
    Entity local = getRidingOrPlayer();
    if (local != null) {
      local.noPhysics = false;
    }
  }

  @SubscribeEvent
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent event) {
    Entity local = getRidingOrPlayer();
    local.noPhysics = true;
    local.setOnGround(false);
    local.fallDistance = 0;
  }
}
