package com.matt.forgehax.util.entity;

import com.matt.forgehax.Globals;
import com.matt.forgehax.mods.commands.FriendsCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import static com.matt.forgehax.Helper.getLocalPlayer;

public class PlayerUtils implements Globals {

  /**
   * Use EntityUtils::isLocalPlayer
   */
  @Deprecated
  public static boolean isLocalPlayer(Entity player) {
    Player localPlayer = getLocalPlayer();
    return localPlayer != null && localPlayer.equals(player);
  }

  public static boolean isFriend(Player player) {
    return player != null && FriendsCommand.isFriend(player.getGameProfile().getName());
  }
}
