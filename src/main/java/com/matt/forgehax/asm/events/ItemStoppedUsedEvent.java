package com.matt.forgehax.asm.events;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class ItemStoppedUsedEvent extends Event {

  private final MultiPlayerGameMode playerController;
  private final Player player;

  public ItemStoppedUsedEvent(MultiPlayerGameMode playerController, Player player) {
    this.playerController = playerController;
    this.player = player;
  }

  public MultiPlayerGameMode getPlayerController() {
    return playerController;
  }

  public Player getPlayer() {
    return player;
  }
}
