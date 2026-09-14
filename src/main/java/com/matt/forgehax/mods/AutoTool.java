package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.PlayerAttackEntityEvent;
import com.matt.forgehax.asm.events.PlayerDamageBlockEvent;
import com.matt.forgehax.util.BlockHelper;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.LocalPlayerInventory;
import com.matt.forgehax.util.entity.LocalPlayerInventory.InvItem;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Comparator;
import java.util.Optional;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;
import static net.minecraft.world.item.enchantment.Enchantments.BLOCK_EFFICIENCY;

@RegisterMod
public class AutoTool extends ToggleMod {

  private static AutoTool instance = null;
  private final Setting<Boolean> tools =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("tools")
          .description("Enables AutoTool when tools")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> weapons =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("weapons")
          .description("Enables AutoTool for weapons")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> revert_back =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("revert-back")
          .description("Revert back to the previous item")
          .defaultTo(true)
          .build();
  private final Setting<Integer> durability_threshold =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("durability-threshold")
          .description(
              "Will filter out items with a damage equal to or less than the threshold. Set to 0 to disable.")
          .defaultTo(0)
          .min(0)
          .max(10000)
          .build();

  public AutoTool() {
    super(Category.PLAYER, "AutoTool", false, "Automatically switch to the best tool");
    instance = this;
  }

  public static AutoTool getInstance() {
    return instance;
  }

  private boolean isInvincible(InvItem item) {
    return item.isNull() || !item.isDamageable();
  }

  private boolean isDurabilityGood(InvItem item) {
    return durability_threshold.get() < 1
        || isInvincible(item)
        || item.getDurability() > durability_threshold.get();
  }

  private boolean isSilkTouchable(InvItem item, BlockState state, BlockPos pos) {
    // TODO(1.20.1): Block#canSilkHarvest has no vanilla/Forge equivalent left; silk touch
    // eligibility is now purely a loot-table condition. Approximated by just checking the
    // enchantment is present; upgrade to a real loot-table check if precision matters.
    return LocalPlayerInventory.getSelected().getIndex() == item.getIndex()
        && getEnchantmentLevel(Enchantments.SILK_TOUCH, item) > 0;
  }

  private double getDigSpeed(InvItem item, BlockState state, BlockPos pos) {
    double str = item.getItemStack().getDestroySpeed(state);
    int eff = getEnchantmentLevel(BLOCK_EFFICIENCY, item);
    return state.getDestroySpeed(getWorld(), pos) > 0.D
        ? Math.max(str + (str > 1.D ? (eff * eff + 1.D) : 0.D), 0.D)
        : 1.D;
  }

  private double getAttackDamage(InvItem item) {
    return Optional.ofNullable(
                       item.getItemStack()
                           .getAttributeModifiers(EquipmentSlot.MAINHAND)
                           .get(Attributes.ATTACK_DAMAGE))
                   .map(at -> at.stream().findAny().map(AttributeModifier::getAmount).orElse(0.D))
                   .orElse(0.D);
  }

  private double getAttackSpeed(InvItem item) {
    return Optional.ofNullable(
                       item.getItemStack()
                           .getAttributeModifiers(EquipmentSlot.MAINHAND)
                           .get(Attributes.ATTACK_DAMAGE))
                   .map(
                       at ->
                           at.stream().findAny().map(AttributeModifier::getAmount).map(Math::abs).orElse(0.D))
                   .orElse(0.D);
  }

  private double getEntityAttackModifier(InvItem item, Entity target) {
    return EnchantmentHelper.getDamageBonus(
        item.getItemStack(),
        Optional.ofNullable(target)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(LivingEntity::getMobType)
                .orElse(MobType.UNDEFINED)
    );
  }

  private double calculateDPS(InvItem item, Entity target) {
    return (getAttackDamage(item) + 1.D + getEntityAttackModifier(item, target))
        / (getAttackSpeed(item) + 1.D);
  }

  private int getEnchantmentLevel(Enchantment enchantment, InvItem item) {
    return EnchantmentHelper.getItemEnchantmentLevel(enchantment, item.getItemStack());
  }

  private InvItem getBestTool(BlockPos pos) {
    InvItem current = LocalPlayerInventory.getSelected();

    if (!BlockHelper.isBlockPlaceable(pos) || getWorld().getBlockState(pos).isAir()) {
      return current;
    }

    final BlockState state = getWorld().getBlockState(pos);
    return LocalPlayerInventory.getHotbarInventory()
                               .stream()
                               .filter(this::isDurabilityGood)
                               .max(
                                   Comparator.<InvItem>comparingDouble(item -> getDigSpeed(item, state, pos))
                                             .thenComparing(item -> isSilkTouchable(item, state, pos))
                                             .thenComparing(this::isInvincible)
                                             .thenComparing(LocalPlayerInventory::getHotbarDistance))
                               .orElse(current);
  }

  private InvItem getBestWeapon(Entity target) {
    InvItem current = LocalPlayerInventory.getSelected();
    return LocalPlayerInventory.getHotbarInventory()
                               .stream()
                               .filter(this::isDurabilityGood)
                               .max(
                                   Comparator.<InvItem>comparingDouble(item -> calculateDPS(item, target))
                                             .thenComparing(item -> getEnchantmentLevel(Enchantments.FIRE_ASPECT, item))
                                             .thenComparing(item -> getEnchantmentLevel(Enchantments.SWEEPING_EDGE, item))
                                             .thenComparing(this::isInvincible)
                                             .thenComparing(LocalPlayerInventory::getHotbarDistance))
                               .orElse(current);
  }

  public void selectBestTool(BlockPos pos) {
    if (isEnabled() && tools.get()) {
      LocalPlayerInventory.setSelected(getBestTool(pos), revert_back.get(), ticks -> ticks > 5);
    }
  }

  public void selectBestWeapon(Entity target) {
    if (isEnabled() && weapons.get()) {
      LocalPlayerInventory.setSelected(
          getBestWeapon(target),
          revert_back.get(),
          ticks -> getLocalPlayer().getAttackStrengthScale(0.f) >= 1.f && ticks > 30
      );
    }
  }

  @SubscribeEvent
  public void onBlockBreak(PlayerDamageBlockEvent event) {
    selectBestTool(event.getPos());
  }

  @SubscribeEvent
  public void onAttackEntity(PlayerAttackEntityEvent event) {
    selectBestWeapon(event.getVictim());
  }
}
