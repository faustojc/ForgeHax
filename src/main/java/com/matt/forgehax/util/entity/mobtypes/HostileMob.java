package com.matt.forgehax.util.entity.mobtypes;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Created on 6/27/2017 by fr1kin
 */
public class HostileMob extends MobType {

  @Override
  public boolean isMobType(Entity entity) {
    return entity instanceof Enemy;
  }

  @Override
  protected MobTypeEnum getMobTypeUnchecked(Entity entity) {
    return MobTypeEnum.HOSTILE;
  }
}
