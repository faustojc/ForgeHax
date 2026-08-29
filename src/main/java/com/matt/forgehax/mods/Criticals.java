package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.asm.events.PlayerAttackEntityEvent;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.MobEffects;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getNetworkManager;

@RegisterMod
public class Criticals extends ToggleMod {

  private volatile boolean restoreSprinting;

  public Criticals() {
    super(Category.COMBAT, "Criticals", false, "Makes attacks critical hits");
  }

  @SubscribeEvent
  public void onAttackEntity(PlayerAttackEntityEvent event) {
    EntityPlayerSP player = getLocalPlayer();
    NetworkManager networkManager = getNetworkManager();

    if (player == null
        || networkManager == null
        || event.getAttacker() != player
        || !player.onGround
        || player.isInWater()
        || player.isInLava()
        || player.isOnLadder()
        || player.isRiding()
        || player.isPotionActive(MobEffects.BLINDNESS)) {
      return;
    }

    if (player.isSprinting()) {
      networkManager.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SPRINTING));
      restoreSprinting = true;
    }

    networkManager.sendPacket(
        new CPacketPlayer.Position(player.posX, player.posY + 0.0625D, player.posZ, false));
    networkManager.sendPacket(
        new CPacketPlayer.Position(player.posX, player.posY, player.posZ, false));
    networkManager.sendPacket(
        new CPacketPlayer.Position(player.posX, player.posY + 0.000011D, player.posZ, false));
    networkManager.sendPacket(
        new CPacketPlayer.Position(player.posX, player.posY, player.posZ, false));
  }

  @SubscribeEvent
  public void onPacketSent(PacketEvent.Outgoing.Post event) {
    if (restoreSprinting
        && event.getPacket() instanceof CPacketUseEntity
        && ((CPacketUseEntity) event.getPacket()).getAction() == CPacketUseEntity.Action.ATTACK
    ) {
      restoreSprinting = false;
      EntityPlayerSP player = getLocalPlayer();
      NetworkManager networkManager = getNetworkManager();
      if (player != null && networkManager != null) {
        networkManager.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_SPRINTING));
      }
    }
  }
}
