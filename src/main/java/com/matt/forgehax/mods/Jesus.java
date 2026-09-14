package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.AddCollisionBoxToListEvent;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.mod.BaseMod;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.matt.forgehax.Helper.*;
import static com.matt.forgehax.util.entity.EntityUtils.isAboveWater;
import static com.matt.forgehax.util.entity.EntityUtils.isInWater;

/**
 * Created by Babbaj on 8/29/2017.
 */
@RegisterMod
public class Jesus extends ToggleMod {

  private static final AABB WATER_WALK_AA =
      new AABB(0.D, 0.D, 0.D, 1.D, 0.99D, 1.D);

  public Jesus() {
    super(Category.PLAYER, "Jesus", false, "Walk on water");
  }

  private static boolean isAboveLand(Entity entity) {
    if (entity == null) {
      return false;
    }

    double y = entity.getY() - 0.01;

    for (int x = Mth.floor(entity.getX()); x < Mth.ceil(entity.getX()); x++) {
      for (int z = Mth.floor(entity.getZ()); z < Mth.ceil(entity.getZ()); z++) {
        BlockPos pos = new BlockPos(x, Mth.floor(y), z);

        if (!getWorld().getBlockState(pos).getCollisionShape(getWorld(), pos).isEmpty()) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isAboveBlock(Entity entity, BlockPos pos) {
    return entity.getY() >= pos.getY();
  }

  @SubscribeEvent
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent event) {
    if (!getModManager().get(FreecamMod.class).map(BaseMod::isEnabled).orElse(false)) {
      if (isInWater(getLocalPlayer()) && !getLocalPlayer().isShiftKeyDown()) {
        Vec3 motion = getLocalPlayer().getDeltaMovement();
        getLocalPlayer().setDeltaMovement(motion.x, 0.1, motion.z);
        Entity vehicle = getLocalPlayer().getVehicle();
        if (vehicle != null && !(vehicle instanceof Boat)) {
          Vec3 vehicleMotion = vehicle.getDeltaMovement();
          vehicle.setDeltaMovement(vehicleMotion.x, 0.3, vehicleMotion.z);
        }
      }
    }
  }

  @SubscribeEvent
  public void onAddCollisionBox(AddCollisionBoxToListEvent event) {
    if (getLocalPlayer() != null
        && (event.getBlock() instanceof LiquidBlock)
        && (EntityUtils.isDrivenByPlayer(event.getEntity())
        || EntityUtils.isLocalPlayer(event.getEntity()))
        && !(event.getEntity() instanceof Boat)
        && !getLocalPlayer().isShiftKeyDown()
        && getLocalPlayer().fallDistance < 3
        && !isInWater(getLocalPlayer())
        && (isAboveWater(getLocalPlayer(), false) || isAboveWater(getRidingEntity(), false))
        && isAboveBlock(getLocalPlayer(), event.getPos())) {
      AABB axisalignedbb = WATER_WALK_AA.move(event.getPos());
      if (event.getEntityBox().intersects(axisalignedbb)) {
        event.getCollidingBoxes().add(axisalignedbb);
      }
      // cancel event, which will stop it from calling the original code
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onPacketSending(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
      ServerboundMovePlayerPacket packet = event.getPacket();
      if (packet.hasPosition()
          && isAboveWater(getLocalPlayer(), true)
          && !isInWater(getLocalPlayer())
          && !isAboveLand(getLocalPlayer())) {
        int ticks = getLocalPlayer().tickCount % 2;
        if (ticks == 0) {
          double y = packet.getY(0) + 0.02D;
          ServerboundMovePlayerPacket replacement = packet.hasRotation()
              ? new ServerboundMovePlayerPacket.PosRot(
                  packet.getX(0), y, packet.getZ(0), packet.getYRot(0), packet.getXRot(0), packet.isOnGround())
              : new ServerboundMovePlayerPacket.Pos(packet.getX(0), y, packet.getZ(0), packet.isOnGround());
          PacketHelper.ignore(replacement);
          getNetworkManager().send(replacement);
          event.setCanceled(true);
        }
      }
    }
  }
}
