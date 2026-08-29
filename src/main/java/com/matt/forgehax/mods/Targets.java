package com.matt.forgehax.mods;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.matt.forgehax.util.command.Options;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.command.callbacks.CallbackData;
import com.matt.forgehax.util.command.exception.CommandExecuteException;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.PlayerUtils;
import com.matt.forgehax.util.entry.ClassEntry;
import com.matt.forgehax.util.mod.BaseMod;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.util.ResourceLocation;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

import static com.matt.forgehax.Helper.getLocalPlayer;

/**
 * Shared target selection for every mod that picks something to attack (Aimbot, KillAura, ...).
 * Only answers "is this entity allowed to be a target"; how far away and how it gets hit stays with
 * the mod doing the attacking.
 */
@RegisterMod
public class Targets extends BaseMod {

  /**
   * Covers wither skulls and dragon fireballs too, both extend EntityFireball
   */
  private static final Class<?>[] MISC_TYPES = {
      EntityArmorStand.class,
      EntityEnderCrystal.class,
      EntityFireball.class,
      EntityShulkerBullet.class,
      EntityBoat.class,
      EntityMinecart.class,
  };

  private static Targets instance;

  private final Setting<Boolean> players =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("players")
          .description("Target players")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> hostile =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("hostile")
          .description("Target hostile mobs (zombies, skeletons, angry neutrals, ...)")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> neutral =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("neutral")
          .description("Target neutral mobs that aren't currently angry (wolves, endermen, ...)")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> passive =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("passive")
          .description("Target passive mobs (cows, villagers, ...)")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> misc =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("misc")
          .description(
              "Target non-living attackable entities (armor stands, shulker bullets, fireballs, ...)")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> teammates =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("teammates")
          .description("Target entities on the same scoreboard team as you")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> friends =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("friends")
          .description("Target players on the global friend list (.friends)")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> pets =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("pets")
          .description("Target pets/mounts/familiars you own, vanilla or modded")
          .defaultTo(false)
          .build();
  private final Setting<ListModes> listMode =
      getCommandStub()
          .builders()
          .<ListModes>newSettingEnumBuilder()
          .name("list-mode")
          .description("How the entity list is applied")
          .defaultTo(ListModes.DISABLED)
          .build();
  private final Options<ClassEntry> entityList =
      getCommandStub()
          .builders()
          .<ClassEntry>newOptionsBuilder()
          .name("list")
          .description("Entity types used by list-mode")
          .factory(ClassEntry::new)
          .supplier(Sets::newConcurrentHashSet)
          .build();

  public Targets() {
    super(Category.COMBAT, "Targets", "Shared target filters for Aimbot, KillAura and friends");
    instance = this;
  }

  /**
   * Everything a targeting mod should check before it considers an entity at all. Range, field of
   * view and visibility are the caller's business.
   */
  public static boolean isValidTarget(Entity entity) {
    return instance != null
        && entity != null
        && entity != getLocalPlayer()
        && EntityUtils.isValidEntity(entity)
        && isAttackable(entity)
        && instance.isAllowed(entity);
  }

  /**
   * If the entity can still be hit right now. Re-check this right before swinging, the target was
   * picked earlier in the tick and may have died since.
   */
  public static boolean isAttackable(Entity entity) {
    if (entity instanceof EntityArmorStand && ((EntityArmorStand) entity).hasMarker()) {
      return false;
    }
    return EntityUtils.isLiving(entity) ? EntityUtils.isAlive(entity) : !entity.isDead;
  }

  private static Optional<ResourceLocation> findEntityName(String name) {
    return EntityList.getEntityNameList()
                     .stream()
                     .filter(rl -> rl.toString().toLowerCase().contains(name))
                     .min(Comparator.comparingInt(rl -> rl.toString().length()));
  }

  /**
   * The filter configuration proper: relationship groups, the friend/team/pet exclusions and the
   * white/blacklist.
   */
  private boolean isAllowed(Entity entity) {
    if (!friends.get()
        && EntityUtils.isPlayer(entity)
        && PlayerUtils.isFriend((EntityPlayer) entity)) {
      return false;
    }
    if (!teammates.get() && EntityUtils.isOnLocalTeam(entity)) {
      return false;
    }
    if (!pets.get() && EntityUtils.isOwnedByLocalPlayer(entity)) {
      return false;
    }

    switch (listMode.get()) {
      case WHITELIST:
        return isListed(entity);
      case BLACKLIST:
        if (isListed(entity)) {
          return false;
        }
        break;
      case DISABLED:
      default:
        break;
    }

    if (isMisc(entity)) {
      return misc.get();
    }

    if (!EntityUtils.isLiving(entity)) {
      return false;
    }

    switch (EntityUtils.getRelationship(entity)) {
      case PLAYER:
        return players.get();
      case HOSTILE:
        return hostile.get();
      case NEUTRAL:
        return neutral.get();
      case FRIENDLY:
        return passive.get();
      case INVALID:
      default:
        return false;
    }
  }

  /**
   * Entities that are worth hitting but aren't mobs. Entity::canBeAttackedWithItem is useless here
   * since it returns true for everything in 1.12 (items, xp orbs, arrows included), so the types
   * are listed explicitly. Anything else non-living has to go through the whitelist.
   */
  private boolean isMisc(Entity entity) {
    for (Class<?> clazz : MISC_TYPES) {
      if (clazz.isInstance(entity)) {
        return true;
      }
    }
    return false;
  }

  private boolean isListed(Entity entity) {
    for (ClassEntry classEntry : entityList) {
      Class<?> clazz = classEntry.getClassInstance();
      if (clazz != null && clazz.isInstance(entity)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void onLoad() {
    entityList
        .builders()
        .newCommandBuilder()
        .name("add")
        .description("Add an entity by its registry name, ex. shulker_bullet")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0).toLowerCase();

              if (Strings.isNullOrEmpty(name)) {
                throw new CommandExecuteException("Empty or null argument");
              }

              Optional<ResourceLocation> match = findEntityName(name);

              if (!match.isPresent()) {
                data.write(String.format("Could not find any entity matching \"%s\"", name));
                data.markFailed();
                return;
              }

              Class<? extends Entity> clazz = EntityList.getClass(match.get());

              if (clazz == null) {
                data.write(String.format("No class registered for \"%s\"", match.get()));
                data.markFailed();
                return;
              }

              entityList.add(new ClassEntry(clazz));
              data.write(String.format("Added \"%s\" (%s)", match.get(), clazz.getSimpleName()));
              data.markSuccess();
            })
        .success(cb -> entityList.serialize())
        .build();

    entityList
        .builders()
        .newCommandBuilder()
        .name("remove")
        .description("Remove an entity from the list")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0).toLowerCase();

              if (Strings.isNullOrEmpty(name)) {
                throw new CommandExecuteException("Empty or null argument");
              }

              Optional<ClassEntry> match =
                  entityList
                      .stream()
                      .filter(entry -> entry.getClassName().toLowerCase().contains(name))
                      .min((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getClassName(), o2.getClassName()));

              if (match.isPresent() && entityList.remove(match.get())) {
                data.write(String.format("Removed \"%s\"", match.get().getClassName()));
                data.markSuccess();
              } else {
                data.write(String.format("Could not find any entry matching \"%s\"", name));
                data.markFailed();
              }
            })
        .success(cb -> entityList.serialize())
        .build();

    entityList
        .builders()
        .newCommandBuilder()
        .name("list")
        .description("List current contents")
        .processor(
            data -> {
              Iterator<ClassEntry> it = entityList.iterator();
              StringBuilder builder = new StringBuilder();
              while (it.hasNext()) {
                builder.append(it.next().getClassName());
                if (it.hasNext()) {
                  builder.append(", ");
                }
              }
              data.write(builder.length() > 0 ? builder.toString() : "empty");
              data.markSuccess();
            })
        .build();
  }

  /**
   * Always on, it holds settings rather than doing anything itself.
   */
  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean isHidden() {
    return false;
  }

  @Override
  protected void onUnload() {
  }

  @Override
  protected void onEnabled() {
  }

  @Override
  protected void onDisabled() {
  }

  @Override
  protected void onBindPressed(CallbackData cb) {
  }

  @Override
  protected void onBindKeyDown(CallbackData cb) {
  }

  enum ListModes {
    /**
     * Only the group filters above are used
     */
    DISABLED,
    /**
     * Only entities in the list are targeted, group filters are ignored
     */
    WHITELIST,
    /**
     * Entities in the list are never targeted
     */
    BLACKLIST,
  }
}
