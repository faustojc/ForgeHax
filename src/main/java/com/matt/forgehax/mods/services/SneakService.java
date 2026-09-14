package com.matt.forgehax.mods.services;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.matt.forgehax.Helper.getLocalPlayer;

@RegisterMod
public class SneakService extends ServiceMod {

  private static SneakService instance;
  private boolean suppressing = false;
  private boolean sneakingClient = false;
  private boolean sneakingServer = false;

  public SneakService() {
    super("SneakService");
    instance = this;
  }

  public static SneakService getInstance() {
    return instance;
  }

  public boolean isSuppressing() {
    return suppressing;
  }

  public void setSuppressing(boolean suppressing) {
    this.suppressing = suppressing;
  }

  public boolean isSneakingClient() {
    return sneakingClient;
  }

  public boolean isSneakingServer() {
    return sneakingServer;
  }

  @SubscribeEvent
  public void onPacketSend(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof ServerboundPlayerCommandPacket) {
      ServerboundPlayerCommandPacket packet = event.getPacket();
      if (getLocalPlayer().getId() == packet.getId()
          && (packet.getAction() == Action.PRESS_SHIFT_KEY
          || packet.getAction() == Action.RELEASE_SHIFT_KEY)
          && !PacketHelper.isIgnored(packet)) {
        sneakingClient = packet.getAction() == Action.PRESS_SHIFT_KEY;
        if (isSuppressing()) {
          event.setCanceled(true);
        } else {
          sneakingServer = sneakingClient;
        }
      }
    }
  }
}
