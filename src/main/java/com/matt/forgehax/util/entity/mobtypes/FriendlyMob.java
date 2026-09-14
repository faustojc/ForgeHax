package com.matt.forgehax.util.entity.mobtypes;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.Villager;

/**
 * Created on 6/27/2017 by fr1kin
 */
public class FriendlyMob extends MobType {

  @Override
  public boolean isMobType(Entity entity) {
    return entity instanceof Animal
        || entity instanceof AmbientCreature
        || entity instanceof WaterAnimal
        || entity instanceof Villager
        || entity instanceof AbstractGolem;
  }

  @Override
  protected MobTypeEnum getMobTypeUnchecked(Entity entity) {
    return MobTypeEnum.FRIENDLY;
  }
}
