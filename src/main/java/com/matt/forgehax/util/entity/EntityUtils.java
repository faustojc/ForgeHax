package com.matt.forgehax.util.entity;

import com.google.common.collect.Maps;
import com.matt.forgehax.Globals;
import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.entity.mobtypes.MobType;
import com.matt.forgehax.util.entity.mobtypes.MobTypeEnum;
import com.matt.forgehax.util.entity.mobtypes.MobTypeRegistry;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.*;

import static com.matt.forgehax.Helper.*;

public class EntityUtils implements Globals {

  private static final String[] OWNER_METHOD_NAMES = {
      "getOwnerId", "getOwnerUniqueId", "getOwnerUUID"
  };
  private static final Map<Class<?>, Optional<Method>> OWNER_METHODS = Maps.newConcurrentMap();
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
    if (entity instanceof EntityPigZombie) {
      // arms raised = aggressive, angry = either game or we have set the anger cooldown
      if (((EntityPigZombie) entity).isArmsRaised() || ((EntityPigZombie) entity).isAngry()) {
        if (!((EntityPigZombie) entity).isAngry()) {
          // set pigmens anger to 400 if it hasn't been angered already
          FastReflection.Fields.EntityPigZombie_angerLevel.set(entity, 400);
        }
        return true;
      }
    } else if (entity instanceof EntityWolf) {
      return ((EntityWolf) entity).isAngry() && !MC.player.equals(((EntityWolf) entity).getOwner());
    } else if (entity instanceof EntityEnderman) {
      return ((EntityEnderman) entity).isScreaming();
    }
    return false;
  }

  /**
   * Check if the mob is an instance of EntityLivingBase
   */
  public static boolean isLiving(Entity entity) {
    return entity instanceof EntityLivingBase;
  }

  /**
   * If the entity is a player
   */
  public static boolean isPlayer(Entity entity) {
    return entity instanceof EntityPlayer;
  }

  public static boolean isLocalPlayer(Entity entity) {
    return Objects.equals(getLocalPlayer(), entity);
  }

  public static boolean isFakeLocalPlayer(Entity entity) {
    return entity != null && entity.getEntityId() == -100;
  }

  public static boolean isValidEntity(Entity entity) {
    Entity riding = getLocalPlayer().getRidingEntity();
    return entity.ticksExisted > 1
        && !isFakeLocalPlayer(entity)
        && (riding == null || !riding.equals(entity));
  }

  public static boolean isAlive(Entity entity) {
    return isLiving(entity) && !entity.isDead && ((EntityLivingBase) (entity)).getHealth() > 0;
  }

  /**
   * If the mob by default wont attack the player, but will if the player attacks it
   */
  public static boolean isNeutralMob(Entity entity) {
    return entity instanceof EntityPigZombie
        || entity instanceof EntityWolf
        || entity instanceof EntityEnderman;
  }

  /**
   * If the mob is friendly (not aggressive)
   */
  public static boolean isFriendlyMob(Entity entity) {
    return (entity.isCreatureType(EnumCreatureType.CREATURE, false)
        && !EntityUtils.isNeutralMob(entity))
        || (entity.isCreatureType(EnumCreatureType.AMBIENT, false) && !isBatsDisabled)
        || entity instanceof EntityVillager
        || entity instanceof EntityIronGolem
        || (isNeutralMob(entity) && !EntityUtils.isMobAggressive(entity));
  }

  /**
   * If the entity is on the same scoreboard team as the local player. Always false if the local
   * player has no team.
   */
  public static boolean isOnLocalTeam(Entity entity) {
    EntityPlayer local = getLocalPlayer();
    return local != null && entity != null && local.isOnSameTeam(entity);
  }

  /**
   * If the entity is a pet/mount/familiar owned by the local player. Only explicit ownership counts:
   * IEntityOwnable, horses, or a modded pet exposing an unambiguously named owner UUID getter.
   * Anything fuzzier (scanning the data manager for our UUID, or a bare getOwner() that mods also
   * use for "who am I angry at") false-positives on every mob currently targeting us, which silently
   * removes hostile and angered neutral mobs from every targeting mod that filters out pets.
   */
  public static boolean isOwnedByLocalPlayer(Entity entity) {
    EntityPlayer local = getLocalPlayer();
    if (local == null || entity == null) {
      return false;
    }

    final UUID uuid = local.getUniqueID();

    if (entity instanceof IEntityOwnable) {
      return uuid.equals(((IEntityOwnable) entity).getOwnerId());
    } else if (entity instanceof AbstractHorse) {
      return uuid.equals(((AbstractHorse) entity).getOwnerUniqueId());
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
    return (entity.isCreatureType(EnumCreatureType.MONSTER, false)
        && !EntityUtils.isNeutralMob(entity))
        || EntityUtils.isMobAggressive(entity);
  }

  /**
   * Find the entities interpolated amount
   */
  public static Vec3d getInterpolatedAmount(Entity entity, double x, double y, double z) {
    return new Vec3d(
        (entity.posX - entity.lastTickPosX) * x,
        (entity.posY - entity.lastTickPosY) * y,
        (entity.posZ - entity.lastTickPosZ) * z
    );
  }

  public static Vec3d getInterpolatedAmount(Entity entity, Vec3d vec) {
    return getInterpolatedAmount(entity, vec.x, vec.y, vec.z);
  }

  public static Vec3d getInterpolatedAmount(Entity entity, double ticks) {
    return getInterpolatedAmount(entity, ticks, ticks, ticks);
  }

  /**
   * Find the entities interpolated position
   */
  public static Vec3d getInterpolatedPos(Entity entity, double ticks) {
    return new Vec3d(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ)
        .add(getInterpolatedAmount(entity, ticks));
  }

  /**
   * Find the entities interpolated eye position
   */
  public static Vec3d getInterpolatedEyePos(Entity entity, double ticks) {
    return getInterpolatedPos(entity, ticks).addVector(0, entity.getEyeHeight(), 0);
  }

  /**
   * Get entities eye position
   */
  public static Vec3d getEyePos(Entity entity) {
    return new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
  }

  /**
   * Squared distance from a point to the nearest surface of a bounding box, 0 if the point is
   * inside it. Reach checks need this instead of a center-to-center distance, since a large mob's
   * center can be several blocks from the part of it that is next to you.
   */
  public static double getDistanceSq(Vec3d point, AxisAlignedBB bb) {
    double dx = Math.max(Math.max(bb.minX - point.x, 0.D), point.x - bb.maxX);
    double dy = Math.max(Math.max(bb.minY - point.y, 0.D), point.y - bb.maxY);
    double dz = Math.max(Math.max(bb.minZ - point.z, 0.D), point.z - bb.maxZ);
    return dx * dx + dy * dy + dz * dz;
  }

  /**
   * Find the center of the entities hit box
   */
  public static Vec3d getOBBCenter(Entity entity) {
    AxisAlignedBB obb = entity.getEntityBoundingBox();
    return new Vec3d(
        (obb.maxX + obb.minX) / 2.D, (obb.maxY + obb.minY) / 2.D, (obb.maxZ + obb.minZ) / 2.D);
  }

  /**
   * Create a trace
   */
  public static RayTraceResult traceEntity(
      World world, Vec3d start, Vec3d end, List<Entity> filter) {
    RayTraceResult result = null;
    double hitDistance = -1;

    for (Entity ent : world.loadedEntityList) {

      if (filter.contains(ent)) {
        continue;
      }

      double distance = start.distanceTo(ent.getPositionVector());
      RayTraceResult trace = ent.getEntityBoundingBox().calculateIntercept(start, end);

      if (trace != null && (hitDistance == -1 || distance < hitDistance)) {
        hitDistance = distance;
        result = trace;
        result.entityHit = ent;
      }
    }

    return result;
  }

  /**
   * Find the entities draw color
   */
  public static int getDrawColor(EntityLivingBase living) {
    if (isPlayer(living)) {
      if (PlayerUtils.isFriend((EntityPlayer) living)) {
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
        entity.posY
            - (packet
            ? 0.03
            : (EntityUtils.isPlayer(entity)
               ? 0.2
               : 0.5)); // increasing this seems to flag more in NCP but needs to be increased
    // so the player lands on solid water

    for (int x = MathHelper.floor(entity.posX); x < MathHelper.ceil(entity.posX); x++) {
      for (int z = MathHelper.floor(entity.posZ); z < MathHelper.ceil(entity.posZ); z++) {
        BlockPos pos = new BlockPos(x, MathHelper.floor(y), z);

        if (getWorld().getBlockState(pos).getBlock() instanceof BlockLiquid) {
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

    double y = entity.posY + 0.01;

    for (int x = MathHelper.floor(entity.posX); x < MathHelper.ceil(entity.posX); x++) {
      for (int z = MathHelper.floor(entity.posZ); z < MathHelper.ceil(entity.posZ); z++) {
        BlockPos pos = new BlockPos(x, (int) y, z);

        if (getWorld().getBlockState(pos).getBlock() instanceof BlockLiquid) {
          return true;
        }
      }
    }

    return false;
  }
}
