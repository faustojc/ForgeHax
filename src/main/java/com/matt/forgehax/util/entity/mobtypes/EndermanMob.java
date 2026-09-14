package com.matt.forgehax.util.entity.mobtypes;

import com.matt.forgehax.util.common.PriorityEnum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;

/**
 * Created on 6/27/2017 by fr1kin
 */
public class EndermanMob extends MobType {

  @Override
  protected PriorityEnum getPriority() {
    return PriorityEnum.LOW;
  }

  @Override
  public boolean isMobType(Entity entity) {
    return entity instanceof EnderMan;
  }

  @Override
  protected MobTypeEnum getMobTypeUnchecked(Entity entity) {
    EnderMan enderman = (EnderMan) entity;
    return enderman.isCreepy() ? MobTypeEnum.HOSTILE : MobTypeEnum.NEUTRAL;
  }
}
