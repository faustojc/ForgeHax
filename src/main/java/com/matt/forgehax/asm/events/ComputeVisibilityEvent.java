package com.matt.forgehax.asm.events;

import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraftforge.eventbus.api.Event;

public class ComputeVisibilityEvent extends Event {

  private final VisGraph visGraph;
  private final VisibilitySet setVisibility;

  public ComputeVisibilityEvent(VisGraph visGraph, VisibilitySet setVisibility) {
    this.visGraph = visGraph;
    this.setVisibility = setVisibility;
  }

  public VisGraph getVisGraph() {
    return visGraph;
  }

  public VisibilitySet getSetVisibility() {
    return setVisibility;
  }
}
