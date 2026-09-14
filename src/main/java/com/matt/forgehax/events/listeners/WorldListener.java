package com.matt.forgehax.events.listeners;

import com.matt.forgehax.events.EntityAddedEvent;
import com.matt.forgehax.events.EntityRemovedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Bridges Forge's client-world entity lifecycle events to ForgeHax's stable
 * entity events. The old 1.12 world listener interface was removed in 1.20.
 */
public final class WorldListener {

  @SubscribeEvent
  public void onEntityAdded(EntityJoinLevelEvent event) {
    if (!event.isCanceled() && event.getLevel().isClientSide()) {
      MinecraftForge.EVENT_BUS.post(new EntityAddedEvent(event.getEntity()));
    }
  }

  @SubscribeEvent
  public void onEntityRemoved(EntityLeaveLevelEvent event) {
    if (event.getLevel().isClientSide()) {
      MinecraftForge.EVENT_BUS.post(new EntityRemovedEvent(event.getEntity()));
    }
  }
}
