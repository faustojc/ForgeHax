package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.asm.events.PlayerAttackEntityEvent;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
    LocalPlayer player = getLocalPlayer();
    Connection networkManager = getNetworkManager();

    if (player == null
        || networkManager == null
        || event.getAttacker() != player
        || !player.onGround()
        || player.isInWater()
        || player.isInLava()
        || player.onClimbable()
        || player.isPassenger()
        || player.hasEffect(MobEffects.BLINDNESS)) {
      return;
    }

    if (player.isSprinting()) {
      networkManager.send(new ServerboundPlayerCommandPacket(
          player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
      restoreSprinting = true;
    }

    networkManager.send(
        new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY() + 0.0625D, player.getZ(), false));
    networkManager.send(
        new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY(), player.getZ(), false));
    networkManager.send(
        new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY() + 0.000011D, player.getZ(), false));
    networkManager.send(
        new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY(), player.getZ(), false));
  }

  @SubscribeEvent
  public void onPacketSent(PacketEvent.Outgoing.Post event) {
    if (restoreSprinting
        && event.getPacket() instanceof ServerboundInteractPacket
        && isAttack((ServerboundInteractPacket) event.getPacket())
    ) {
      restoreSprinting = false;
      LocalPlayer player = getLocalPlayer();
      Connection networkManager = getNetworkManager();
      if (player != null && networkManager != null) {
        networkManager.send(new ServerboundPlayerCommandPacket(
            player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
      }
    }
  }

  private static boolean isAttack(ServerboundInteractPacket packet) {
    final boolean[] attack = {false};
    packet.dispatch(new ServerboundInteractPacket.Handler() {
      @Override
      public void onInteraction(InteractionHand hand) {
      }

      @Override
      public void onInteraction(InteractionHand hand, Vec3 location) {
      }

      @Override
      public void onAttack() {
        attack[0] = true;
      }
    });
    return attack[0];
  }
}
