package com.matt.forgehax.mods;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@RegisterMod
public class AutoReconnectMod extends ToggleMod {

  public static boolean hasAutoLogged =
      false; // used to disable autoreconnecting without disabling the entire mod
  private static ServerData lastConnectedServer;
  public final Setting<Double> delay =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("delay")
          .description("Delay between each reconnect attempt")
          .defaultTo(5.D)
          .build();

  // Only one DisconnectedScreen can be showing at a time, so a single set of fields is enough
  // to track the countdown instead of subclassing the screen (its parent/reason fields are
  // private in 1.20.1 with no accessor left).
  private DisconnectedScreen activeScreen = null;
  private long reconnectTime = 0;
  private Button reconnectButton = null;

  public AutoReconnectMod() {
    super(Category.MISC, "AutoReconnect", false, "Automatically reconnects to server");
  }

  public void updateLastConnectedServer() {
    ServerData data = MC.getCurrentServer();
    if (data != null) {
      lastConnectedServer = data;
    }
  }

  public ServerData getLastConnectedServerData() {
    return lastConnectedServer != null ? lastConnectedServer : MC.getCurrentServer();
  }

  public long getTimeUntilReconnect() {
    return reconnectTime - System.currentTimeMillis();
  }

  public double getTimeUntilReconnectInSeconds() {
    return (double) getTimeUntilReconnect() / 1000.D;
  }

  public String getFormattedReconnectText() {
    return String.format("Reconnecting (%.1f)...", getTimeUntilReconnectInSeconds());
  }

  private void reconnect() {
    ServerData data = getLastConnectedServerData();
    if (data != null && activeScreen != null) {
      ConnectScreen.startConnecting(
          activeScreen, MC, ServerAddress.parseString(data.ip), data, false);
    }
    activeScreen = null;
    reconnectButton = null;
  }

  @SubscribeEvent
  public void onScreenInit(ScreenEvent.Init.Post event) {
    if (!hasAutoLogged && event.getScreen() instanceof DisconnectedScreen) {
      updateLastConnectedServer();
      if (getLastConnectedServerData() != null) {
        activeScreen = (DisconnectedScreen) event.getScreen();
        reconnectTime = System.currentTimeMillis() + (long) (delay.get() * 1000);
        reconnectButton =
            Button.builder(Component.literal(getFormattedReconnectText()), btn -> reconnect())
                  .bounds(
                      activeScreen.width / 2 - 100,
                      activeScreen.height / 2 + 50,
                      200,
                      20
                  )
                  .build();
        event.addListener(reconnectButton);
      }
    }
  }

  @SubscribeEvent
  public void onScreenRender(ScreenEvent.Render.Post event) {
    if (activeScreen != null && event.getScreen() == activeScreen) {
      if (reconnectButton != null) {
        reconnectButton.setMessage(Component.literal(getFormattedReconnectText()));
      }
      if (System.currentTimeMillis() >= reconnectTime) {
        reconnect();
      }
    }
  }

  @SubscribeEvent
  public void onScreenClosing(ScreenEvent.Closing event) {
    if (event.getScreen() == activeScreen) {
      activeScreen = null;
      reconnectButton = null;
    }
  }

  @SubscribeEvent
  public void onWorldLoad(LevelEvent.Load event) {
    // we got on the server or stopped joining, now undo queue
    hasAutoLogged = false; // make mod work when you rejoin
  }

  @SubscribeEvent
  public void onWorldUnload(LevelEvent.Unload event) {
    updateLastConnectedServer();
  }
}
