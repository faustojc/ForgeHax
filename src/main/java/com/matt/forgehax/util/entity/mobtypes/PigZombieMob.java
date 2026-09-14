package com.matt.forgehax.util.entity.mobtypes;

import com.matt.forgehax.util.common.PriorityEnum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.ZombifiedPiglin;

/**
 * Created on 6/27/2017 by fr1kin
 */
public class PigZombieMob extends MobType {

  @Override
  protected PriorityEnum getPriority() {
    return PriorityEnum.LOW;
  }

  @Override
  public boolean isMobType(Entity entity) {
    return entity instanceof ZombifiedPiglin;
  }

  @Override
  protected MobTypeEnum getMobTypeUnchecked(Entity entity) {
    ZombifiedPiglin zombie = (ZombifiedPiglin) entity;
    return (zombie.isAggressive() || zombie.getRemainingPersistentAngerTime() > 0)
        ? MobTypeEnum.HOSTILE
        : MobTypeEnum.NEUTRAL;
  }
}
