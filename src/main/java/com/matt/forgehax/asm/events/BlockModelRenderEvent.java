package com.matt.forgehax.asm.events;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created on 5/5/2017 by fr1kin
 */
public class BlockModelRenderEvent extends Event {

  private final BlockAndTintGetter blockAccess;
  private final BakedModel bakedModel;
  private final BlockState blockState;
  private final BlockPos blockPos;
  private final BufferBuilder buffer;
  private final boolean checkSides;
  private final long rand;

  public BlockModelRenderEvent(
      BlockAndTintGetter worldIn,
      BakedModel modelIn,
      BlockState stateIn,
      BlockPos posIn,
      BufferBuilder buffer,
      boolean checkSides,
      long rand
  ) {
    this.blockAccess = worldIn;
    this.bakedModel = modelIn;
    this.blockState = stateIn;
    this.blockPos = posIn;
    this.buffer = buffer;
    this.checkSides = checkSides;
    this.rand = rand;
  }

  public BlockAndTintGetter getBlockAccess() {
    return blockAccess;
  }

  public BakedModel getBakedModel() {
    return bakedModel;
  }

  public BlockState getBlockState() {
    return blockState;
  }

  public BlockPos getBlockPos() {
    return blockPos;
  }

  public BufferBuilder getBuffer() {
    return buffer;
  }

  public boolean isCheckSides() {
    return checkSides;
  }

  public long getRand() {
    return rand;
  }
}
