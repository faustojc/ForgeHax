package com.matt.forgehax.mods;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.world.level.block.SoulSandBlock;

@RegisterMod
public class NoSlowdown extends ToggleMod {

  public NoSlowdown() {
    super(Category.PLAYER, "NoSlowDown", false, "Disables block slowdown");
  }

  // Both flags are consumed by mixins: MixinEntity (soul sand speed factor, via
  // ForgeHaxHooks.getBlockSpeedFactor) and MixinLocalPlayer (item-use slowdown in aiStep).
  @Override
  public void onEnabled() {
    ForgeHaxHooks.isNoSlowDownActivated = true;
    try {
      ForgeHaxHooks.LIST_BLOCK_FILTER.add(SoulSandBlock.class);
    } catch (Exception e) {
    }
  }

  @Override
  public void onDisabled() {
    ForgeHaxHooks.isNoSlowDownActivated = false;
    try {
      ForgeHaxHooks.LIST_BLOCK_FILTER.remove(SoulSandBlock.class);
    } catch (Exception e) {
    }
  }
}
