package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getNetworkManager;

@RegisterMod
public class NoFallMod extends ToggleMod {

  private float lastFallDistance = 0;

  public NoFallMod() {
    super(Category.PLAYER, "NoFall", false, "Prevents fall damage from being taken");
  }

  @SubscribeEvent
  public void onPacketSend(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof ServerboundMovePlayerPacket
        && !(event.getPacket() instanceof ServerboundMovePlayerPacket.Rot)
        && !PacketHelper.isIgnored(event.getPacket())) {
      ServerboundMovePlayerPacket packetPlayer = (ServerboundMovePlayerPacket) event.getPacket();
      if (packetPlayer.isOnGround() && lastFallDistance >= 4) {
        ServerboundMovePlayerPacket packet =
            new ServerboundMovePlayerPacket.PosRot(
                packetPlayer.getX(0),
                1337 + packetPlayer.getY(0),
                packetPlayer.getZ(0),
                packetPlayer.getYRot(0),
                packetPlayer.getXRot(0),
                true
            );
        ServerboundMovePlayerPacket reposition =
            new ServerboundMovePlayerPacket.PosRot(
                packetPlayer.getX(0),
                packetPlayer.getY(0),
                packetPlayer.getZ(0),
                packetPlayer.getYRot(0),
                packetPlayer.getXRot(0),
                true
            );
        PacketHelper.ignore(packet);
        PacketHelper.ignore(reposition);
        getNetworkManager().send(packet);
        getNetworkManager().send(reposition);
        lastFallDistance = 0;
      } else {
        lastFallDistance = getLocalPlayer().fallDistance;
      }
    }
  }
}
