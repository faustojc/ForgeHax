package com.matt.forgehax;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.matt.forgehax.mods.services.MainMenuGuiService.CommandInputGui;
import com.matt.forgehax.util.FileManager;
import com.matt.forgehax.util.command.CommandGlobal;
import com.matt.forgehax.util.mod.loader.ModManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Scanner;

/**
 * Created on 4/25/2017 by fr1kin
 */
public class Helper implements Globals {

  public static CommandGlobal getGlobalCommand() {
    return CommandGlobal.getInstance();
  }

  public static Minecraft getMinecraft() {
    return MC;
  }

  public static ModManager getModManager() {
    return ModManager.getInstance();
  }

  public static FileManager getFileManager() {
    return FileManager.getInstance();
  }

  public static Logger getLog() {
    return LOGGER;
  }

  public static Entity getRenderEntity() {
    return MC.getCameraEntity();
  }

  @Nullable
  public static LocalPlayer getLocalPlayer() {
    return MC.player;
  }

  @Nullable
  public static Entity getRidingEntity() {
    LocalPlayer player = getLocalPlayer();
    return player == null ? null : player.getVehicle();
  }

  public static Optional<Entity> getOptionalRidingEntity() {
    return Optional.ofNullable(getRidingEntity());
  }

  // Returns the riding entity if present, otherwise the local player
  @Nullable
  public static Entity getRidingOrPlayer() {
    Entity riding = getRidingEntity();
    return riding != null ? riding : getLocalPlayer();
  }

  @Nullable
  public static ClientLevel getWorld() {
    return MC.level;
  }

  public static Level getWorld(Entity entity) {
    return entity.level();
  }

  public static Level getWorld(BlockEntity blockEntity) {
    return blockEntity.getLevel();
  }

  @Nullable
  public static Connection getNetworkManager() {
    return MC.getConnection() == null ? null : MC.getConnection().getConnection();
  }

  @Nullable
  public static MultiPlayerGameMode getPlayerController() {
    return MC.gameMode;
  }

  public static void printMessageNaked(
      String startWith, String message, Style firstStyle, Style secondStyle) {
    if (!Strings.isNullOrEmpty(message)) {
      if (message.contains("\n")) {
        Scanner scanner = new Scanner(message);
        scanner.useDelimiter("\n");
        Style s1 = firstStyle;
        Style s2 = secondStyle;
        while (scanner.hasNext()) {
          printMessageNaked(startWith, scanner.next(), s1, s2);
          // alternate between colors each newline
          Style cpy = s1;
          s1 = s2;
          s2 = cpy;
        }
        scanner.close();
      } else {
        Component text = Component.literal(startWith + message.replace("\r", ""))
            .withStyle(firstStyle);
        outputMessage(text);
      }
    }
  }

  // private function that is ultimately used to output the message
  private static void outputMessage(Component message) {
    LocalPlayer player = getLocalPlayer();
    if (player != null) {
      player.displayClientMessage(message, false);
    } else if (MC.screen instanceof CommandInputGui) {
      ((CommandInputGui) MC.screen).print(message.getString());
    }
  }

  public static void printMessageNaked(String append, String message, Style style) {
    printMessageNaked(append, message, style, style);
  }

  public static void printMessageNaked(String append, String message) {
    printMessageNaked(
        append,
        message,
        Style.EMPTY.withColor(ChatFormatting.WHITE),
        Style.EMPTY.withColor(ChatFormatting.GRAY)
    );
  }

  public static void printMessageNaked(String message) {
    printMessageNaked("", message);
  }

  // Will append '[FH] ' in front
  public static void printMessage(String message) {
    if (!Strings.isNullOrEmpty(message)) {
      printMessageNaked("[FH] " + message);
    }
  }

  public static void printMessage(String format, Object... args) {
    printMessage(String.format(format, args));
  }

  private static MutableComponent getFormattedText(
      String text, ChatFormatting color,
      boolean bold, boolean italic
  ) {
    return Component.literal(text.replace("\r", ""))
        .withStyle(Style.EMPTY.withColor(color).withBold(bold).withItalic(italic));
  }

  public static void printInform(String format, Object... args) {
    outputMessage(
        getFormattedText("[ForgeHax]", ChatFormatting.GREEN, true, false)
            .append(getFormattedText(
                " " + String.format(format, args).trim(),
                ChatFormatting.GRAY, false, false
            ))
    );
  }

  public static void printWarning(String format, Object... args) {
    outputMessage(
        getFormattedText("[ForgeHax]", ChatFormatting.YELLOW, true, false)
            .append(getFormattedText(
                " " + String.format(format, args).trim(),
                ChatFormatting.GRAY, false, false
            ))
    );
  }

  public static void printError(String format, Object... args) {
    outputMessage(
        getFormattedText("[ForgeHax]", ChatFormatting.RED, true, false)
            .append(getFormattedText(
                " " + String.format(format, args).trim(),
                ChatFormatting.GRAY, false, false
            ))
    );
  }

  public static void printStackTrace(Throwable t) {
    getLog().error(Throwables.getStackTraceAsString(t));
  }

  public static void handleThrowable(Throwable t) {
    getLog().error(String.format(
        "[%s] %s",
        t.getClass().getSimpleName(),
        Strings.nullToEmpty(t.getMessage())
    ));

    if (t.getCause() != null) {
      handleThrowable(t.getCause());
    }
    printStackTrace(t);
  }

  public static void reloadChunks() {
    if (getWorld() != null && getLocalPlayer() != null) {
      MC.execute(() -> {
        if (getWorld() != null && getLocalPlayer() != null) {
          MC.levelRenderer.allChanged();
        }
      });
    }
  }

  public static void reloadChunksHard() {
    MC.execute(() -> {
      if (getWorld() != null && getLocalPlayer() != null) {
        MC.levelRenderer.allChanged();
      }
    });
  }
}
