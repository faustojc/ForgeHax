package com.matt.forgehax.asm.events;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created on 5/11/2017 by fr1kin
 */
public class WorldRendererDeallocatedEvent extends Event {

  private final Object generator;
  private final RenderChunk renderChunk;

  public WorldRendererDeallocatedEvent(
      Object generator, RenderChunk renderChunk) {
    this.generator = generator;
    this.renderChunk = renderChunk;
  }

  public Object getGenerator() {
    return generator;
  }

  public RenderChunk getRenderChunk() {
    return renderChunk;
  }
}
