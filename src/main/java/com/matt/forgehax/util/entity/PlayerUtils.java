package com.matt.forgehax.util.entity;

import com.matt.forgehax.Globals;
import com.matt.forgehax.mods.commands.FriendsCommand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import static com.matt.forgehax.Helper.getLocalPlayer;

public class PlayerUtils implements Globals {

  /**
   * Use EntityUtils::isLocalPlayer
   */
  @Deprecated
  public static boolean isLocalPlayer(Entity player) {
    EntityPlayer localPlayer = getLocalPlayer();
    return localPlayer != null && localPlayer.equals(player);
  }

  public static boolean isFriend(EntityPlayer player) {
    return player != null && FriendsCommand.isFriend(player.getName());
  }
}
