package com.matt.forgehax.asm.events;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created on 5/10/2017 by fr1kin
 */
public class LoadRenderersEvent extends Event {

  private final ViewArea viewFrustum;
  private final ChunkRenderDispatcher renderDispatcher;

  public LoadRenderersEvent(ViewArea viewFrustum, ChunkRenderDispatcher renderDispatcher) {
    this.viewFrustum = viewFrustum;
    this.renderDispatcher = renderDispatcher;
  }

  public ViewArea getViewFrustum() {
    return viewFrustum;
  }

  public ChunkRenderDispatcher getRenderDispatcher() {
    return renderDispatcher;
  }
}
