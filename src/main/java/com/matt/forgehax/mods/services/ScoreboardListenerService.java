package com.matt.forgehax.mods.services;

import com.google.common.util.concurrent.FutureCallback;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.events.PlayerConnectEvent;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.PlayerInfo;
import com.matt.forgehax.util.entity.PlayerInfoHelper;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.mojang.authlib.GameProfile;
import joptsimple.internal.Strings;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getLog;

/**
 * Created on 7/18/2017 by fr1kin
 */
@RegisterMod
public class ScoreboardListenerService extends ServiceMod {

  private final Setting<Integer> wait =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("wait")
          .description("Time to wait after joining world")
          .defaultTo(5000)
          .build();
  private final Setting<Integer> retries =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("retries")
          .description("Number of times to attempt retries on failure")
          .defaultTo(1)
          .build();

  private final SimpleTimer timer = new SimpleTimer();

  private boolean ignore = false;

  public ScoreboardListenerService() {
    super("ScoreboardListenerService", "Listens for player joining and leaving");
  }

  private void fireEvents(boolean joined, PlayerInfo info, GameProfile profile) {
    if (ignore || info == null) {
      return;
    }
    if (joined) {
      MinecraftForge.EVENT_BUS.post(new PlayerConnectEvent.Join(info, profile));
    } else {
      MinecraftForge.EVENT_BUS.post(new PlayerConnectEvent.Leave(info, profile));
    }
  }

  @SubscribeEvent
  public void onClientConnect(ClientPlayerNetworkEvent.LoggingIn event) {
    ignore = false;
  }

  @SubscribeEvent
  public void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
    ignore = false;
  }

  @SubscribeEvent
  public void onPacketIn(PacketEvent.Incoming.Pre event) {
    if (ignore && timer.isStarted() && timer.hasTimeElapsed(wait.get())) {
      ignore = false;
    }

    if (!ignore && event.getPacket() instanceof ClientboundCustomPayloadPacket) {
      ignore = true;
      timer.start();
    } else if (ignore && event.getPacket() instanceof ClientboundLevelChunkWithLightPacket) {
      ignore = false;
      timer.reset();
    }
  }

  // player-list (tab list) add: 1.19.3+ splits this out of the old SPacketPlayerListItem
  @SubscribeEvent
  public void onPlayerListAdd(PacketEvent.Incoming.Pre event) {
    if (event.getPacket() instanceof ClientboundPlayerInfoUpdatePacket) {
      final ClientboundPlayerInfoUpdatePacket packet = event.getPacket();
      if (!packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
        return;
      }

      packet
          .entries()
          .stream()
          .filter(Objects::nonNull)
          .filter(
              data ->
                  !Strings.isNullOrEmpty(data.profile().getName())
                      || data.profile().getId() != null)
          .forEach(
              data -> {
                final String name = data.profile().getName();
                final UUID id = data.profile().getId();
                final AtomicInteger retries = new AtomicInteger(this.retries.get());
                PlayerInfoHelper.registerWithCallback(
                    id,
                    name,
                    new FutureCallback<PlayerInfo>() {
                      @Override
                      public void onSuccess(@Nullable PlayerInfo result) {
                        fireEvents(true, result, data.profile());
                      }

                      @Override
                      public void onFailure(Throwable t) {
                        if (retries.getAndDecrement() > 0) {
                          getLog()
                              .warn(
                                  "Failed to lookup "
                                      + name
                                      + "/"
                                      + id.toString()
                                      + ", retrying ("
                                      + retries.get()
                                      + ")...");
                          PlayerInfoHelper.registerWithCallback(data.profile().getId(), name, this);
                        } else {
                          t.printStackTrace();
                          PlayerInfoHelper.generateOfflineWithCallback(name, this);
                        }
                      }
                    }
                );
              });
    }
  }

  // player-list (tab list) remove: now a separate packet carrying only UUIDs
  @SubscribeEvent
  public void onPlayerListRemove(PacketEvent.Incoming.Pre event) {
    if (event.getPacket() instanceof ClientboundPlayerInfoRemovePacket) {
      final ClientboundPlayerInfoRemovePacket packet = event.getPacket();
      packet
          .profileIds()
          .forEach(
              id -> {
                net.minecraft.client.multiplayer.PlayerInfo netInfo =
                    getLocalPlayer() != null && getLocalPlayer().connection != null
                        ? getLocalPlayer().connection.getPlayerInfo(id)
                        : null;
                if (netInfo == null) {
                  return;
                }
                final GameProfile profile = netInfo.getProfile();
                final String name = profile.getName();
                final AtomicInteger retries = new AtomicInteger(this.retries.get());
                PlayerInfoHelper.registerWithCallback(
                    id,
                    name,
                    new FutureCallback<PlayerInfo>() {
                      @Override
                      public void onSuccess(@Nullable PlayerInfo result) {
                        fireEvents(false, result, profile);
                      }

                      @Override
                      public void onFailure(Throwable t) {
                        if (retries.getAndDecrement() > 0) {
                          getLog()
                              .warn(
                                  "Failed to lookup "
                                      + name
                                      + "/"
                                      + id.toString()
                                      + ", retrying ("
                                      + retries.get()
                                      + ")...");
                          PlayerInfoHelper.registerWithCallback(id, name, this);
                        } else {
                          t.printStackTrace();
                          PlayerInfoHelper.generateOfflineWithCallback(name, this);
                        }
                      }
                    }
                );
              });
    }
  }
}
