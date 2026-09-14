package com.matt.forgehax.util.entity;

import com.matt.forgehax.Globals;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.entity.mobtypes.MobType;
import com.matt.forgehax.util.entity.mobtypes.MobTypeEnum;
import com.matt.forgehax.util.entity.mobtypes.MobTypeRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.matt.forgehax.Helper.*;

public class EntityUtils implements Globals {

  private static final String[] OWNER_METHOD_NAMES = {
      "getOwnerId", "getOwnerUniqueId", "getOwnerUUID"
  };
  private static final Map<Class<?>, Optional<Method>> OWNER_METHODS = new ConcurrentHashMap<>();
  public static boolean isBatsDisabled = false;

  public static MobTypeEnum getRelationship(Entity entity) {
    if (entity instanceof AbstractClientPlayer) {
      return MobTypeEnum.PLAYER;
    } else {
      // check special cases first
      for (MobType type : MobTypeRegistry.getSortedSpecialMobTypes()) {
        if (type.isMobType(entity)) {
          return type.getMobType(entity);
        }
      }
      // this code will continue if no special was found
      if (MobTypeRegistry.HOSTILE.isMobType(entity)) {
        return MobTypeEnum.HOSTILE;
      } else if (MobTypeRegistry.FRIENDLY.isMobType(entity)) {
        return MobTypeEnum.FRIENDLY;
      } else {
        return MobTypeEnum.HOSTILE; // default to hostile
      }
    }
  }

  /**
   * Checks if the mob could be possibly hostile towards us (we can't detect their attack target
   * easily) Current entities: PigZombie: Aggressive if arms are raised, when arms are put down a
   * internal timer is slowly ticked down from 400 Wolf: Aggressive if the owner isn't the local
   * player and the wolf is angry Enderman: Aggressive if making screaming sounds
   */
  public static boolean isMobAggressive(Entity entity) {
    if (entity instanceof ZombifiedPiglin) {
      // aggressive is the modern equivalent of raised arms; persistent anger covers angry piglins
      ZombifiedPiglin piglin = (ZombifiedPiglin) entity;
      boolean angry = piglin.getRemainingPersistentAngerTime() > 0;
      if (piglin.isAggressive() || angry) {
        if (!angry) {
          // set piglins' anger to 400 if it hasn't been angered already
          piglin.setRemainingPersistentAngerTime(400);
        }
        return true;
      }
    } else if (entity instanceof Wolf) {
      Wolf wolf = (Wolf) entity;
      return wolf.getRemainingPersistentAngerTime() > 0
          && !Objects.equals(MC.player, wolf.getOwner());
    } else if (entity instanceof EnderMan) {
      return ((EnderMan) entity).isCreepy();
    }
    return false;
  }

  /**
   * Check if the mob is an instance of LivingEntity
   */
  public static boolean isLiving(Entity entity) {
    return entity instanceof LivingEntity;
  }

  /**
   * If the entity is a player
   */
  public static boolean isPlayer(Entity entity) {
    return entity instanceof Player;
  }

  public static boolean isLocalPlayer(Entity entity) {
    return Objects.equals(getLocalPlayer(), entity);
  }

  public static boolean isFakeLocalPlayer(Entity entity) {
    return entity != null && entity.getId() == -100;
  }

  public static boolean isValidEntity(Entity entity) {
    Entity riding = getLocalPlayer().getVehicle();
    return entity.tickCount > 1
        && !isFakeLocalPlayer(entity)
        && (riding == null || !riding.equals(entity));
  }

  public static boolean isAlive(Entity entity) {
    return isLiving(entity) && entity.isAlive() && ((LivingEntity) entity).getHealth() > 0;
  }

  /**
   * If the mob by default wont attack the player, but will if the player attacks it
   */
  public static boolean isNeutralMob(Entity entity) {
    return entity instanceof ZombifiedPiglin || entity instanceof Wolf || entity instanceof EnderMan;
  }

  /**
   * If the mob is friendly (not aggressive)
   */
  public static boolean isFriendlyMob(Entity entity) {
    return ((entity instanceof Animal
            || entity instanceof AmbientCreature
            || entity instanceof WaterAnimal)
        && !EntityUtils.isNeutralMob(entity))
        || (entity instanceof AmbientCreature && !isBatsDisabled)
        || entity instanceof Villager
        || entity instanceof AbstractGolem
        || (isNeutralMob(entity) && !EntityUtils.isMobAggressive(entity));
  }

  /**
   * If the entity is on the same scoreboard team as the local player. Always false if the local
   * player has no team.
   */
  public static boolean isOnLocalTeam(Entity entity) {
    Player local = getLocalPlayer();
    return local != null && entity != null && local.isAlliedTo(entity);
  }

  /**
   * If the entity is a pet/mount/familiar owned by the local player. Only explicit ownership counts:
   * OwnableEntity, horses, or a modded pet exposing an unambiguously named owner UUID getter.
   * Anything fuzzier (scanning the data manager for our UUID, or a bare getOwner() that mods also
   * use for "who am I angry at") false-positives on every mob currently targeting us, which silently
   * removes hostile and angered neutral mobs from every targeting mod that filters out pets.
   */
  public static boolean isOwnedByLocalPlayer(Entity entity) {
    Player local = getLocalPlayer();
    if (local == null || entity == null) {
      return false;
    }

    final UUID uuid = local.getUUID();

    if (entity instanceof OwnableEntity) {
      return uuid.equals(((OwnableEntity) entity).getOwnerUUID());
    } else if (entity instanceof AbstractHorse) {
      return uuid.equals(((AbstractHorse) entity).getOwnerUUID());
    } else {
      return uuid.equals(getModdedOwnerId(entity));
    }
  }

  /**
   * Modded pets usually keep the MCP/forge naming for their owner getter since they aren't
   * obfuscated
   */
  @Nullable
  private static UUID getModdedOwnerId(Entity entity) {
    Optional<Method> method =
        OWNER_METHODS.computeIfAbsent(entity.getClass(), EntityUtils::findOwnerMethod);
    if (!method.isPresent()) {
      return null;
    }
    try {
      Object result = method.get().invoke(entity);
      if (result instanceof UUID) {
        return (UUID) result;
      }
    } catch (Throwable t) {
      // getter blew up, treat the entity as unowned
    }
    return null;
  }

  private static Optional<Method> findOwnerMethod(Class<?> clazz) {
    for (String name : OWNER_METHOD_NAMES) {
      try {
        Method method = clazz.getMethod(name);
        if (UUID.class.equals(method.getReturnType())) {
          method.setAccessible(true);
          return Optional.of(method);
        }
      } catch (Throwable t) {
        // no such method, try the next name
      }
    }
    return Optional.empty();
  }

  /**
   * If the mob is hostile
   */
  public static boolean isHostileMob(Entity entity) {
    return ((entity instanceof net.minecraft.world.entity.monster.Enemy)
        && !EntityUtils.isNeutralMob(entity))
        || EntityUtils.isMobAggressive(entity);
  }

  /**
   * Find the entities interpolated amount
   */
  public static Vec3 getInterpolatedAmount(Entity entity, double x, double y, double z) {
    return new Vec3(
        (entity.getX() - entity.xOld) * x,
        (entity.getY() - entity.yOld) * y,
        (entity.getZ() - entity.zOld) * z
    );
  }

  public static Vec3 getInterpolatedAmount(Entity entity, Vec3 vec) {
    return getInterpolatedAmount(entity, vec.x, vec.y, vec.z);
  }

  public static Vec3 getInterpolatedAmount(Entity entity, double ticks) {
    return getInterpolatedAmount(entity, ticks, ticks, ticks);
  }

  /**
   * Find the entities interpolated position
   */
  public static Vec3 getInterpolatedPos(Entity entity, double ticks) {
    return new Vec3(entity.xOld, entity.yOld, entity.zOld)
        .add(getInterpolatedAmount(entity, ticks));
  }

  /**
   * Find the entities interpolated eye position
   */
  public static Vec3 getInterpolatedEyePos(Entity entity, double ticks) {
    return getInterpolatedPos(entity, ticks).add(0, entity.getEyeHeight(), 0);
  }

  /**
   * Get entities eye position
   */
  public static Vec3 getEyePos(Entity entity) {
    return new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
  }

  /**
   * Squared distance from a point to the nearest surface of a bounding box, 0 if the point is
   * inside it. Reach checks need this instead of a center-to-center distance, since a large mob's
   * center can be several blocks from the part of it that is next to you.
   */
  public static double getDistanceSq(Vec3 point, AABB bb) {
    double dx = Math.max(Math.max(bb.minX - point.x, 0.D), point.x - bb.maxX);
    double dy = Math.max(Math.max(bb.minY - point.y, 0.D), point.y - bb.maxY);
    double dz = Math.max(Math.max(bb.minZ - point.z, 0.D), point.z - bb.maxZ);
    return dx * dx + dy * dy + dz * dz;
  }

  /**
   * Find the center of the entities hit box
   */
  public static Vec3 getOBBCenter(Entity entity) {
    AABB obb = entity.getBoundingBox();
    return new Vec3(
        (obb.maxX + obb.minX) / 2.D, (obb.maxY + obb.minY) / 2.D, (obb.maxZ + obb.minZ) / 2.D);
  }

  /**
   * Create a trace
   */
  public static EntityHitResult traceEntity(
      Level world, Vec3 start, Vec3 end, List<Entity> filter) {
    EntityHitResult result = null;
    double hitDistance = -1;

    for (Entity ent : world.getEntities((Entity) null, new AABB(start, end), e -> true)) {
      if (filter.contains(ent)) {
        continue;
      }

      double distance = start.distanceTo(ent.position());
      Optional<Vec3> hit = ent.getBoundingBox().clip(start, end);

      if (hit.isPresent() && (hitDistance == -1 || distance < hitDistance)) {
        hitDistance = distance;
        result = new EntityHitResult(ent, hit.get());
      }
    }

    return result;
  }

  /**
   * Find the entities draw color
   */
  public static int getDrawColor(LivingEntity living) {
    if (isPlayer(living)) {
      if (PlayerUtils.isFriend((Player) living)) {
        return Colors.GREEN.toBuffer();
      } else {
        return Colors.RED.toBuffer();
      }
    } else if (isHostileMob(living)) {
      return Colors.ORANGE.toBuffer();
    } else if (isFriendlyMob(living)) {
      return Colors.GREEN.toBuffer();
    } else {
      return Colors.WHITE.toBuffer();
    }
  }

  public static boolean isDrivenByPlayer(Entity entityIn) {
    return getLocalPlayer() != null && entityIn != null && entityIn == getRidingEntity();
  }

  public static boolean isAboveWater(Entity entity) {
    return isAboveWater(entity, false);
  }

  public static boolean isAboveWater(Entity entity, boolean packet) {
    if (entity == null) {
      return false;
    }

    double y =
        entity.getY()
            - (packet
            ? 0.03
            : (EntityUtils.isPlayer(entity)
               ? 0.2
               : 0.5)); // increasing this seems to flag more in NCP but needs to be increased
    // so the player lands on solid water

    for (int x = Mth.floor(entity.getX()); x < Mth.ceil(entity.getX()); x++) {
      for (int z = Mth.floor(entity.getZ()); z < Mth.ceil(entity.getZ()); z++) {
        BlockPos pos = new BlockPos(x, Mth.floor(y), z);

        if (getWorld().getFluidState(pos).is(FluidTags.WATER)) {
          return true;
        }
      }
    }

    return false;
  }

  public static boolean isInWater(Entity entity) {
    if (entity == null) {
      return false;
    }

    double y = entity.getY() + 0.01;

    for (int x = Mth.floor(entity.getX()); x < Mth.ceil(entity.getX()); x++) {
      for (int z = Mth.floor(entity.getZ()); z < Mth.ceil(entity.getZ()); z++) {
        BlockPos pos = new BlockPos(x, (int) y, z);

        if (getWorld().getFluidState(pos).is(FluidTags.WATER)) {
          return true;
        }
      }
    }

    return false;
  }
}
