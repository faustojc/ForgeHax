package com.matt.forgehax.asm.events;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.eventbus.api.Event;

public class PlayerDamageBlockEvent extends Event {

  private final MultiPlayerGameMode playerController;
  private final BlockPos pos;
  private final Direction side;

  public PlayerDamageBlockEvent(
      MultiPlayerGameMode playerController, BlockPos pos, Direction side) {
    this.playerController = playerController;
    this.pos = pos;
    this.side = side;
  }

  public MultiPlayerGameMode getPlayerController() {
    return playerController;
  }

  public BlockPos getPos() {
    return pos;
  }

  public Direction getSide() {
    return side;
  }
}
