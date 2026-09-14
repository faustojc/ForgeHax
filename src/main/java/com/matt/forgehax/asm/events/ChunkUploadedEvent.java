package com.matt.forgehax.asm.events;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created on 5/7/2017 by fr1kin
 */
public class ChunkUploadedEvent extends Event {

  private final RenderChunk renderChunk;
  private final BufferBuilder.RenderedBuffer buffer;

  public ChunkUploadedEvent(RenderChunk renderChunk, BufferBuilder.RenderedBuffer BufferBuilder) {
    this.renderChunk = renderChunk;
    this.buffer = BufferBuilder;
  }

  public RenderChunk getRenderChunk() {
    return renderChunk;
  }

  public BufferBuilder.RenderedBuffer getBuffer() {
    return buffer;
  }
}
