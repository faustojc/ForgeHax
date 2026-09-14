package com.matt.forgehax.asm.events;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created on 11/10/2016 by fr1kin
 */
public class BlockRenderEvent extends Event {

  private final BlockPos pos;
  private final BlockState state;
  private final BlockAndTintGetter access;
  private final BufferBuilder buffer;

  public BlockRenderEvent(
      BlockPos pos, BlockState state, BlockAndTintGetter access, BufferBuilder buffer) {
    this.pos = pos;
    this.state = state;
    this.access = access;
    this.buffer = buffer;
  }

  public BlockPos getPos() {
    return pos;
  }

  public BlockAndTintGetter getAccess() {
    return access;
  }

  public BlockState getState() {
    return state;
  }

  public BufferBuilder getBuffer() {
    return buffer;
  }
}
