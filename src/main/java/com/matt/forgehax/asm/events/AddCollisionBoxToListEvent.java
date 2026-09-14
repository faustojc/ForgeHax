package com.matt.forgehax.asm.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

/**
 * Created on 4/9/2017 by fr1kin
 */
@Cancelable
public class AddCollisionBoxToListEvent extends Event {

  private final Block block;
  private final BlockState state;
  private final Level world;
  private final BlockPos pos;
  private final AABB entityBox;
  private final List<AABB> collidingBoxes;
  private final Entity entity;
  private final boolean bool;

  public AddCollisionBoxToListEvent(
      Block block,
      BlockState state,
      Level worldIn,
      BlockPos pos,
      AABB entityBox,
      List<AABB> collidingBoxes,
      Entity entityIn,
      boolean bool
  ) {
    this.block = block;
    this.state = state;
    this.world = worldIn;
    this.pos = pos;
    this.entityBox = entityBox;
    this.collidingBoxes = collidingBoxes;
    this.entity = entityIn;
    this.bool = bool;
  }

  public Block getBlock() {
    return block;
  }

  public BlockState getState() {
    return state;
  }

  public Level getWorld() {
    return world;
  }

  public BlockPos getPos() {
    return pos;
  }

  public AABB getEntityBox() {
    return entityBox;
  }

  public List<AABB> getCollidingBoxes() {
    return collidingBoxes;
  }

  public Entity getEntity() {
    return entity;
  }

  public boolean isBool() {
    return bool;
  }
}
