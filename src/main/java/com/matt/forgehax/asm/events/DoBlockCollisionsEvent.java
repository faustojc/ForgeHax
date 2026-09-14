package com.matt.forgehax.asm.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class DoBlockCollisionsEvent extends EntityEvent {

  private final BlockPos pos;
  private final BlockState state;

  public DoBlockCollisionsEvent(Entity entity, BlockPos pos, BlockState state) {
    super(entity);
    this.pos = pos;
    this.state = state;
  }

  public BlockPos getPos() {
    return pos;
  }

  public BlockState getState() {
    return state;
  }
}
