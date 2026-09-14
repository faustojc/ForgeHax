package com.matt.forgehax.asm.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created by Babbaj on 9/20/2017.
 */
public class SchematicaPlaceBlockEvent extends Event {

  private final ItemStack item;
  private final BlockPos pos;
  private final Vec3 vec;
  private final Direction side;

  public SchematicaPlaceBlockEvent(ItemStack itemIn, BlockPos posIn, Vec3 vecIn, Direction sideIn) {
    this.item = itemIn;
    this.pos = posIn;
    this.vec = vecIn;
    this.side = sideIn;
  }

  public ItemStack getItem() {
    return this.item;
  }

  public BlockPos getPos() {
    return this.pos;
  }

  public Vec3 getVec() {
    return this.vec;
  }

  public Direction getSide() {
    return this.side;
  }
}
