package com.matt.forgehax.asm.events;

import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Created on 6/15/2017 by fr1kin
 */
public class LocalPlayerUpdateMovementEvent extends Event {

  private final LocalPlayer localPlayer;

  private LocalPlayerUpdateMovementEvent(LocalPlayer localPlayer) {
    this.localPlayer = localPlayer;
  }

  public LocalPlayer getLocalPlayer() {
    return localPlayer;
  }

  @Cancelable
  public static class Pre extends LocalPlayerUpdateMovementEvent {

    public Pre(LocalPlayer localPlayer) {
      super(localPlayer);
    }
  }

  public static class Post extends LocalPlayerUpdateMovementEvent {

    public Post(LocalPlayer localPlayer) {
      super(localPlayer);
    }
  }
}
