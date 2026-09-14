package com.matt.forgehax.mods.services;

import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Created on 6/14/2017 by fr1kin
 */
@RegisterMod
public class PigmenAngerCooldownService extends ServiceMod {

  public PigmenAngerCooldownService() {
    super("PigmenAngerCooldownService");
  }

  @SubscribeEvent
  public void onUpdate(LivingEvent.LivingTickEvent event) {
    if (event.getEntity() instanceof ZombifiedPiglin piglin) {
      // EntityPigZombie's private angerLevel field is now NeutralMob's public
      // remainingPersistentAngerTime; "arms raised" (attacking) maps to having a target.
      if (piglin.getTarget() != null) {
        piglin.setRemainingPersistentAngerTime(400);
      } else if (piglin.getRemainingPersistentAngerTime() > 0) {
        piglin.setRemainingPersistentAngerTime(piglin.getRemainingPersistentAngerTime() - 1);
      }
    }
  }
}
