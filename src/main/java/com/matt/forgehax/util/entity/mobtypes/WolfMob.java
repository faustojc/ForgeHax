package com.matt.forgehax.util.entity.mobtypes;

import com.matt.forgehax.util.common.PriorityEnum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Wolf;

/**
 * Created on 6/27/2017 by fr1kin
 */
public class WolfMob extends MobType {

  @Override
  protected PriorityEnum getPriority() {
    return PriorityEnum.LOW;
  }

  @Override
  public boolean isMobType(Entity entity) {
    return entity instanceof Wolf;
  }

  @Override
  protected MobTypeEnum getMobTypeUnchecked(Entity entity) {
    Wolf wolf = (Wolf) entity;
    return wolf.getRemainingPersistentAngerTime() > 0
        ? MobTypeEnum.HOSTILE
        : MobTypeEnum.NEUTRAL;
  }
}
