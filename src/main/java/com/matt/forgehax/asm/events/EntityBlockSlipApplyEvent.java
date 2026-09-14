package com.matt.forgehax.asm.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

public class EntityBlockSlipApplyEvent extends Event {

  private final Stage stage;
  private final LivingEntity entityLivingBase;
  private final BlockState blockStateUnder;
  private final float defaultSlipperiness;
  private float slipperiness;

  public EntityBlockSlipApplyEvent(
      Stage stage,
      LivingEntity entityLivingBase,
      BlockState blockStateUnder,
      float defaultSlipperiness
  ) {
    this.stage = stage;
    this.entityLivingBase = entityLivingBase;
    this.blockStateUnder = blockStateUnder;
    this.defaultSlipperiness = defaultSlipperiness;
    this.slipperiness = defaultSlipperiness;
  }

  public Stage getStage() {
    return stage;
  }

  public LivingEntity getEntityLivingBase() {
    return entityLivingBase;
  }

  public BlockState getBlockStateUnder() {
    return blockStateUnder;
  }

  public float getDefaultSlipperiness() {
    return defaultSlipperiness;
  }

  public float getSlipperiness() {
    return slipperiness;
  }

  public void setSlipperiness(float slipperiness) {
    this.slipperiness = slipperiness;
  }

  public enum Stage {
    FIRST,
    SECOND,
  }
}
