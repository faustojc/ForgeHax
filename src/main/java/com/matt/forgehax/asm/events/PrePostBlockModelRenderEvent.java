package com.matt.forgehax.asm.events;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created on 5/5/2017 by fr1kin
 */
public class PrePostBlockModelRenderEvent extends Event {

  private final RenderChunk renderChunk;
  private final BufferBuilder buffer;
  private final Vec3 pos;
  private final State state;

  public PrePostBlockModelRenderEvent(
      RenderChunk renderChunk, BufferBuilder BufferBuilder, State state, Vec3 pos) {
    this.renderChunk = renderChunk;
    this.buffer = BufferBuilder;
    this.state = state;
    this.pos = pos;
  }

  public PrePostBlockModelRenderEvent(
      RenderChunk renderChunk, BufferBuilder BufferBuilder, State state, BlockPos pos) {
    this(renderChunk, BufferBuilder, state, new Vec3(pos.getX(), pos.getY(), pos.getZ()));
  }

  public PrePostBlockModelRenderEvent(
      RenderChunk renderChunk,
      BufferBuilder BufferBuilder,
      State state,
      float x,
      float y,
      float z
  ) {
    this(renderChunk, BufferBuilder, state, new Vec3(x, y, z));
  }

  public RenderChunk getRenderChunk() {
    return renderChunk;
  }

  public BufferBuilder getBuffer() {
    return buffer;
  }

  public State getState() {
    return state;
  }

  public Vec3 getPos() {
    return pos;
  }

  public enum State {
    PRE,
    POST,
  }
}
