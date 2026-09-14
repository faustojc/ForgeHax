package com.matt.forgehax.asm.events;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraftforge.eventbus.api.Event;

public class PlayerSyncItemEvent extends Event {

  private final MultiPlayerGameMode playerController;

  public PlayerSyncItemEvent(MultiPlayerGameMode playerController) {
    this.playerController = playerController;
  }

  public MultiPlayerGameMode getPlayerController() {
    return playerController;
  }
}
