package com.matt.forgehax.asm.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created on 2/10/2018 by fr1kin
 */
@Cancelable
public class WorldCheckLightForEvent extends Event {

  private final LightLayer enumSkyBlock;
  private final BlockPos pos;

  public WorldCheckLightForEvent(LightLayer enumSkyBlock, BlockPos pos) {
    this.enumSkyBlock = enumSkyBlock;
    this.pos = pos;
  }

  public LightLayer getEnumSkyBlock() {
    return enumSkyBlock;
  }

  public BlockPos getPos() {
    return pos;
  }
}
