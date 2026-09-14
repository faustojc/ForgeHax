package com.matt.forgehax.asm.events;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class PlayerAttackEntityEvent extends Event {

  private final MultiPlayerGameMode playerController;
  private final Player attacker;
  private final Entity victim;

  public PlayerAttackEntityEvent(
      MultiPlayerGameMode playerController, Player attacker, Entity victim) {
    this.playerController = playerController;
    this.attacker = attacker;
    this.victim = victim;
  }

  public MultiPlayerGameMode getPlayerController() {
    return playerController;
  }

  public Player getAttacker() {
    return attacker;
  }

  public Entity getVictim() {
    return victim;
  }
}
