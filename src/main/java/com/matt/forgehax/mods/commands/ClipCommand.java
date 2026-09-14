package com.matt.forgehax.mods.commands;

import com.matt.forgehax.Helper;
import com.matt.forgehax.util.SafeConverter;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.CommandBuilders;
import com.matt.forgehax.util.mod.CommandMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import static com.matt.forgehax.Helper.*;

/**
 * Created by Babbaj on 4/12/2018.
 */
@RegisterMod
public class ClipCommand extends CommandMod {

  public ClipCommand() {
    super("ClipCommand");
  }

  // teleport to absolute position
  private void setPosition(double x, double y, double z) {
    final Entity local = Helper.getRidingOrPlayer();
    local.moveTo(x, y, z);
    if (local instanceof LocalPlayer) {
      getNetworkManager()
          .send(
              new ServerboundMovePlayerPacket.Pos(
                  local.getX(), local.getY(), local.getZ(), MC.player.onGround()));
    } else {
      getNetworkManager().send(new ServerboundMoveVehiclePacket(local));
    }
  }

  // teleport vertically by some offset
  private void offsetY(double yOffset) {
    Entity local = Helper.getRidingOrPlayer();
    setPosition(local.getX(), local.getY() + yOffset, local.getZ());
  }

  @RegisterCommand
  public Command clip(CommandBuilders builders) {
    return builders
        .newCommandBuilder()
        .name("clip")
        .description("Teleport vertically")
        // .requiredArgs(1)
        .processor(
            data -> {
              try {
                switch (data.getArgumentCount()) {
                  case 1: {
                    final double y = Double.parseDouble(data.getArgumentAsString(0));
                    MC.execute(() -> {
                      if (getWorld() == null || getLocalPlayer() == null) {
                        return;
                      }

                      Entity local = getRidingOrPlayer();
                      if (local == null) {
                        return;
                      }

                      setPosition(0, local.getY() + y, 0);
                    });
                    break;
                  }
                  case 3: {
                    final double x = Double.parseDouble(data.getArgumentAsString(0));
                    final double y = Double.parseDouble(data.getArgumentAsString(1));
                    final double z = Double.parseDouble(data.getArgumentAsString(2));
                    MC.execute(() -> {
                      if (getWorld() == null || getLocalPlayer() == null) {
                        return;
                      }

                      Entity local = getRidingOrPlayer();
                      if (local == null) {
                        return;
                      }

                      setPosition(local.getX() + x, local.getY() + y, local.getZ() + z);
                    });
                    break;
                  }
                  default:
                    Helper.printMessage("Invalid number of arguments: expected 1 or 3");
                }
              } catch (NumberFormatException e) {
                Helper.printMessage("Failed to parse input");
              }
            })
        .build();
  }

  @RegisterCommand
  public Command vclip(CommandBuilders builders) {
    return builders.newCommandBuilder()
                   .name("vclip")
                   .description("Vertical clip")
                   .requiredArgs(1)
                   .processor(data -> {
                     if (getWorld() == null || getLocalPlayer() == null) {
                       printWarning("Not in game");
                       return;
                     }
                     final double y = SafeConverter.toDouble(data.getArgumentAsString(0));
                     MC.execute(() -> offsetY(y));
                   })
                   .build();
  }

  @RegisterCommand
  public Command forward(CommandBuilders builders) {
    return builders.newCommandBuilder()
                   .name("forward")
                   .description("Forward clip")
                   .requiredArgs(1)
                   .processor(data -> {
                     if (getWorld() == null || getLocalPlayer() == null) {
                       printWarning("Not in game");
                       return;
                     }
                     final double units = SafeConverter.toDouble(data.getArgumentAsString(0));
                     MC.execute(() -> {
                       Vec3 dir = getLocalPlayer().getLookAngle().normalize();
                       setPosition(
                           getLocalPlayer().getX() + (dir.x * units), getLocalPlayer().getY(),
                           getLocalPlayer().getZ() + (dir.z * units)
                       );
                     });
                   })
                   .build();
  }
}
