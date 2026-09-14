package com.matt.forgehax.util.entity;

import com.matt.forgehax.Globals;
import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.mods.services.SneakService;
import com.matt.forgehax.util.Switch;
import com.matt.forgehax.util.math.Angle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static com.matt.forgehax.Helper.*;

/**
 * Class for dealing with the local player only
 */
public class LocalPlayerUtils implements Globals {

  private static final Switch FLY_SWITCH = new Switch("PlayerFlying") {
    @Override
    protected void onEnabled() {
      MC.execute(() -> {
        if (getLocalPlayer() == null) {
          return;
        }

        Abilities abilities = getLocalPlayer().getAbilities();
        abilities.mayfly = true;
        abilities.flying = true;
      });
    }

    @Override
    protected void onDisabled() {
      MC.execute(() -> {
        Player player = getLocalPlayer();

        if (player == null) {
          return;
        }

        Abilities gameModeAbilities = new Abilities();
        GameType gameType = getPlayerController() == null
            ? GameType.SURVIVAL
            : getPlayerController().getPlayerMode();
        gameType.updatePlayerAbilities(gameModeAbilities);

        Abilities abilities = player.getAbilities();
        abilities.mayfly = gameModeAbilities.mayfly;
        abilities.flying = gameModeAbilities.mayfly && abilities.flying;
        abilities.setFlyingSpeed(gameModeAbilities.getFlyingSpeed());
      });
    }
  };

  /**
   * Gets the players current view angles
   */
  public static Angle getViewAngles() {
    return PositionRotationManager.getState().getRenderClientViewAngles();
  }

  public static Angle getServerViewAngles() {
    return PositionRotationManager.getState().getRenderServerViewAngles();
  }

  public static Vec3 getVelocity() {
    return getLocalPlayer().getDeltaMovement();
  }

  public static boolean isSneaking() {
    return getLocalPlayer().isShiftKeyDown();
  }

  public static boolean setSneaking(boolean sneak) {
    boolean old = isSneaking();
    getLocalPlayer().setShiftKeyDown(sneak);
    if (getLocalPlayer().input != null) {
      getLocalPlayer().input.shiftKeyDown = sneak;
    }
    return old;
  }

  public static void setSneakingSuppression(boolean suppress) {
    SneakService.getInstance().setSuppressing(suppress);
  }

  public static Vec3 getEyePos() {
    return EntityUtils.getEyePos(getLocalPlayer());
  }

  public static Vec3 getDirectionVector() {
    return getViewAngles().getDirectionVector().normalize();
  }

  public static Vec3 getServerDirectionVector() {
    return getServerViewAngles().getDirectionVector().normalize();
  }

  public static HitResult getViewTrace() {
    return MC.hitResult;
  }

  public static HitResult getMouseOverBlockTrace() {
    if (MC.hitResult == null || getWorld() == null) {
      return null;
    }
    return MC.hitResult.getType() == HitResult.Type.BLOCK
        ? MC.hitResult
        : Optional.of(MC.hitResult)
            .filter(tr -> tr.getLocation() != null)
            .filter(tr -> !getWorld().getBlockState(BlockPos.containing(tr.getLocation())).isAir())
            .orElse(null);
  }

  public static HitResult getViewTrace(
      Entity entity, Vec3 direction, float partialTicks, double reach, double reachAttack) {
    if (entity == null || getWorld() == null) {
      return null;
    }

    Vec3 eyes = entity.getEyePosition(partialTicks);
    HitResult trace = entity.pick(reach, partialTicks, false);
    Vec3 lookDir = eyes.add(direction.scale(reach));

    double hitDistance = trace == null ? reachAttack : trace.getLocation().distanceTo(eyes);
    Entity hitEntity = null;
    Vec3 hitEntityVec = null;

    AABB searchBox = entity.getBoundingBox()
        .expandTowards(direction.scale(reach))
        .inflate(1.D);
    for (Entity ent : getWorld().getEntities(
        entity,
        searchBox,
        candidate -> candidate != null
            && !candidate.isSpectator()
            && candidate.canBeCollidedWith())) {
      AABB bb = ent.getBoundingBox().inflate(ent.getPickRadius());
      Optional<Vec3> clip = bb.clip(eyes, lookDir);
      if (bb.contains(eyes)) {
        if (hitDistance > 0.D) {
          hitEntity = ent;
          hitEntityVec = clip.orElse(eyes);
          hitDistance = 0.D;
        }
      } else if (clip.isPresent()) {
        Vec3 hit = clip.get();
        double dist = eyes.distanceTo(hit);
        if (dist < hitDistance || hitDistance == 0.D) {
          if (entity.isPassengerOfSameVehicle(ent)) {
            if (hitDistance == 0.D) {
              hitEntity = ent;
              hitEntityVec = hit;
            }
          } else {
            hitEntity = ent;
            hitEntityVec = hit;
            hitDistance = dist;
          }
        }
      }
    }

    if (hitEntity != null && reach > 3.D && eyes.distanceTo(hitEntityVec) > 3.D) {
      return new BlockHitResult(hitEntityVec, Direction.UP, BlockPos.containing(hitEntityVec), false);
    } else if (hitEntity != null && trace == null && hitDistance < reachAttack) {
      return new EntityHitResult(hitEntity, hitEntityVec);
    } else {
      return trace;
    }
  }

  public static boolean isInReach(Vec3 start, Vec3 end) {
    if (getPlayerController() == null) {
      return false;
    }
    double reach = getPlayerController().getPickRange();
    return start.distanceToSqr(end) < reach * reach;
  }

  public static Switch getFlySwitch() {
    return FLY_SWITCH;
  }
}
