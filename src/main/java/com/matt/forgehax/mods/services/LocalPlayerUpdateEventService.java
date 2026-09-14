package com.matt.forgehax.mods.services;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

/**
 * Created on 6/14/2017 by fr1kin
 */
@RegisterMod
public class LocalPlayerUpdateEventService extends ServiceMod {

  public LocalPlayerUpdateEventService() {
    super("LocalPlayerUpdateEventService");
  }

  @SubscribeEvent
  public void onUpdate(LivingEvent.LivingTickEvent event) {
    LivingEntity entity = event.getEntity();
    if (getWorld() != null
        && entity.level().isClientSide()
        && entity.equals(getLocalPlayer())) {
      Event ev = new LocalPlayerUpdateEvent(entity);
      MinecraftForge.EVENT_BUS.post(ev);
      event.setCanceled(ev.isCanceled());
    }
  }
}
