package com.matt.forgehax.asm.events;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created on 5/9/2017 by fr1kin
 */
public class AddRenderChunkEvent extends Event {

  private final RenderChunk renderChunk;
  private final RenderType blockRenderLayer;

  public AddRenderChunkEvent(RenderChunk renderChunk, RenderType blockRenderLayer) {
    this.renderChunk = renderChunk;
    this.blockRenderLayer = blockRenderLayer;
  }

  public RenderChunk getRenderChunk() {
    return renderChunk;
  }

  public RenderType getBlockRenderLayer() {
    return blockRenderLayer;
  }
}
