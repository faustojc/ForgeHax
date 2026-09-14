package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.Switch.Handle;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.LocalPlayerUtils;
import com.matt.forgehax.util.key.Bindings;
import com.matt.forgehax.util.math.Angle;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

/**
 * Created on 9/3/2016 by fr1kin
 */
@RegisterMod
public class FreecamMod extends ToggleMod {

  private final Setting<Double> speed =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("speed")
          .description("Movement speed")
          .defaultTo(0.05D)
          .build();

  private final Handle flying = LocalPlayerUtils.getFlySwitch().createHandle(getModName());

  private Vec3 pos = Vec3.ZERO;
  private Angle angle = Angle.ZERO;

  private boolean isRidingEntity;
  private Entity ridingEntity;

  private RemotePlayer originalPlayer;

  public FreecamMod() {
    super(Category.PLAYER, "Freecam", false, "Freecam mode");
  }

  @Override
  public void onEnabled() {
    if (getLocalPlayer() == null || getWorld() == null) {
      return;
    }

    if (isRidingEntity = getLocalPlayer().isPassenger()) {
      ridingEntity = getLocalPlayer().getVehicle();
      getLocalPlayer().stopRiding();
    } else {
      pos = getLocalPlayer().position();
    }

    angle = LocalPlayerUtils.getViewAngles();

    originalPlayer = new RemotePlayer(getWorld(), getLocalPlayer().getGameProfile());
    originalPlayer.copyPosition(getLocalPlayer());
    originalPlayer.setYHeadRot(getLocalPlayer().getYHeadRot());
    originalPlayer.getInventory().replaceWith(getLocalPlayer().getInventory());
    // TODO(1.20.1): Player#inventoryMenu is final with no clone helper, so the dummy player's
    // container state (open crafting/inventory screen) can no longer be mirrored from here.
    originalPlayer.setId(-100);
    getWorld().addPlayer(-100, originalPlayer);
  }

  @Override
  public void onDisabled() {
    flying.disable();

    if (getLocalPlayer() == null || originalPlayer == null) {
      return;
    }

    getLocalPlayer().moveTo(pos.x, pos.y, pos.z, angle.getYaw(), angle.getPitch());
    getWorld().removeEntity(-100, Entity.RemovalReason.DISCARDED);
    originalPlayer = null;

    getLocalPlayer().noPhysics = false;
    getLocalPlayer().setDeltaMovement(Vec3.ZERO);

    if (isRidingEntity) {
      getLocalPlayer().startRiding(ridingEntity, true);
      ridingEntity = null;
    }
  }

  @SubscribeEvent
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent event) {
    if (getLocalPlayer() == null) {
      return;
    }

    flying.enable();
    getLocalPlayer().getAbilities().setFlyingSpeed(speed.getAsFloat());
    getLocalPlayer().noPhysics = true;
    getLocalPlayer().setOnGround(false);
    getLocalPlayer().fallDistance = 0;

    if (!Bindings.forward.isPressed()
        && !Bindings.back.isPressed()
        && !Bindings.left.isPressed()
        && !Bindings.right.isPressed()
        && !Bindings.jump.isPressed()
        && !Bindings.sneak.isPressed()) {
      getLocalPlayer().setDeltaMovement(Vec3.ZERO);
    }
  }

  @SubscribeEvent
  public void onPacketSend(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof ServerboundMovePlayerPacket
        || event.getPacket() instanceof ServerboundPlayerInputPacket) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onPacketReceived(PacketEvent.Incoming.Pre event) {
    if (originalPlayer == null || getLocalPlayer() == null) {
      return;
    }

    if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
      ClientboundPlayerPositionPacket packet = event.getPacket();
      pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
      angle = Angle.degrees(packet.getXRot(), packet.getYRot());
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onWorldLoad(LevelEvent.Load event) {
    if (originalPlayer == null || getLocalPlayer() == null) {
      return;
    }

    pos = getLocalPlayer().position();
    angle = LocalPlayerUtils.getViewAngles();
  }

  @SubscribeEvent
  public void onEntityRender(RenderLivingEvent.Pre<?, ?> event) {
    if (originalPlayer != null
        && getLocalPlayer() != null
        && getLocalPlayer().equals(event.getEntity())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onRenderTag(RenderNameTagEvent event) {
    if (originalPlayer != null
        && getLocalPlayer() != null
        && getLocalPlayer().equals(event.getEntity())) {
      event.setContent(Component.empty());
    }
  }

  private static class DummyPlayer extends RemotePlayer {

    public DummyPlayer(ClientLevel levelIn, GameProfile gameProfileIn) {
      super(levelIn, gameProfileIn);
    }

    @Override
    public void tick() {
    }

    @Override
    public void aiStep() {
    }
  }
}
