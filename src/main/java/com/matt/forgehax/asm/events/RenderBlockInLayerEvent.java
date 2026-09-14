package com.matt.forgehax.asm.events;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

public class RenderBlockInLayerEvent extends Event {

  private final Block block;
  private final BlockState state;
  private final RenderType compareToLayer;
  private RenderType layer;

  public RenderBlockInLayerEvent(
      Block block, BlockState state, RenderType layer, RenderType compareToLayer) {
    this.block = block;
    this.state = state;
    this.layer = layer;
    this.compareToLayer = compareToLayer;
  }

  public Block getBlock() {
    return block;
  }

  public RenderType getLayer() {
    return layer;
  }

  public void setLayer(RenderType layer) {
    this.layer = layer;
  }

  public RenderType getCompareToLayer() {
    return compareToLayer;
  }

  public BlockState getState() {
    return state;
  }
}
