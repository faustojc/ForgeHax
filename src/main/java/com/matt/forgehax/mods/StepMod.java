package com.matt.forgehax.mods;

import com.google.common.collect.Lists;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.matt.forgehax.Helper.*;

@RegisterMod
public class StepMod extends ToggleMod {

  private static final float DEFAULT_STEP_HEIGHT = 0.6f;

  private final Setting<Boolean> entityStep =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("entity-step")
          .description("entitystep")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> unstep =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("unstep")
          .description("step down instead of falling")
          .defaultTo(false)
          .build();
  private boolean wasOnGround = false;  private final Setting<Float> stepHeight =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("height")
          .description("how high you can step")
          .defaultTo(1.2f)
          .min(0f)
          .changed(__ -> MC.execute(() -> {
            if (isEnabled()) {
              LocalPlayer player = getLocalPlayer();
              if (player != null) {
                updateStepHeight(player);
              }
            }
          }))
          .build();
  private ServerboundMovePlayerPacket previousPositionPacket = null;
  public StepMod() {
    super(Category.PLAYER, "Step", false, "Step up blocks");
  }

  private void updateStepHeight(LocalPlayer player) {
    player.setMaxUpStep(player.onGround() ? stepHeight.get() : DEFAULT_STEP_HEIGHT);
  }

  private void unstep(LocalPlayer player) {
    AABB range = player.getBoundingBox().expandTowards(0, -stepHeight.get(), 0)
                       .contract(0, player.getBbHeight(), 0);

    if (player.level().noCollision(player, range)) {
      return;
    }

    AtomicReference<Double> newY = new AtomicReference<>(0D);
    player.level().getBlockCollisions(player, range)
          .forEach(shape -> newY.set(Math.max(newY.get(), shape.bounds().maxY)));
    player.absMoveTo(player.getX(), newY.get(), player.getZ());
  }

  private void updateUnstep(LocalPlayer player) {
    try {
      if (unstep.get() && wasOnGround && !player.onGround() && player.getDeltaMovement().y <= 0) {
        unstep(player);
      }
    } finally {
      wasOnGround = player.onGround();
    }
  }

  @SubscribeEvent
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent event) {
    LocalPlayer player = (LocalPlayer) event.getEntity();
    if (player == null) {
      return;
    }

    updateStepHeight(player);
    updateUnstep(player);

    if (getRidingEntity() != null) {
      if (entityStep.getAsBoolean()) {
        getRidingEntity().setMaxUpStep(256);
      } else {
        getRidingEntity().setMaxUpStep(1);
      }
    }
  }

  @SubscribeEvent
  public void onPacketSending(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof ServerboundMovePlayerPacket.Pos
        || event.getPacket() instanceof ServerboundMovePlayerPacket.PosRot) {
      ServerboundMovePlayerPacket packetPlayer = event.getPacket();
      if (previousPositionPacket != null && !PacketHelper.isIgnored(event.getPacket())) {
        double diffY = packetPlayer.getY(0.f) - previousPositionPacket.getY(0.f);
        // y difference must be positive
        // greater than 1, but less than 1.5
        if (diffY > DEFAULT_STEP_HEIGHT && diffY <= 1.2491870787) {
          List<Packet<?>> sendList = Lists.newArrayList();
          // if this is true, this must be a step
          // now to send additional packets to get around NCP
          double x = previousPositionPacket.getX(0.D);
          double y = previousPositionPacket.getY(0.D);
          double z = previousPositionPacket.getZ(0.D);
          sendList.add(new ServerboundMovePlayerPacket.Pos(x, y + 0.4199999869D, z, true));
          sendList.add(new ServerboundMovePlayerPacket.Pos(x, y + 0.7531999805D, z, true));
          sendList.add(
              new ServerboundMovePlayerPacket.Pos(
                  packetPlayer.getX(0.f),
                  packetPlayer.getY(0.f),
                  packetPlayer.getZ(0.f),
                  packetPlayer.isOnGround()
              ));
          for (Packet<?> toSend : sendList) {
            PacketHelper.ignore(toSend);
            getNetworkManager().send(toSend);
          }
          event.setCanceled(true);
        }
      }
      previousPositionPacket = event.getPacket();
    }
  }

  @Override
  protected void onEnabled() {
    LocalPlayer player = getLocalPlayer();
    if (player != null) {
      wasOnGround = player.onGround();
    }
  }

  @Override
  public void onDisabled() {
    LocalPlayer player = getLocalPlayer();
    if (player != null) {
      player.setMaxUpStep(DEFAULT_STEP_HEIGHT);
    }

    if (getRidingEntity() != null) {
      getRidingEntity().setMaxUpStep(1);
    }
  }

  @Override
  public String getDebugDisplayText() {
    return String.format(
        "%s[%s%s]",
        super.getDisplayText(),
        stepHeight.get().toString(),
        unstep.get() ? "+unstep" : ""
    );
  }




}
