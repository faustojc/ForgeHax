package com.matt.forgehax.mods;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.math.AlignHelper.Align;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

@RegisterMod
public class CoordsHud extends HudMod {

  private final Setting<Boolean> translate =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("translate")
          .description("show corresponding Nether or Overworld coords")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> multiline =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("multiline")
          .description("show translated coords above")
          .defaultTo(true)
          .build();
  double thisX;
  double thisY;
  double thisZ;
  double otherX;
  double otherZ;

  public CoordsHud() {
    super(Category.RENDER, "CoordsHUD", false, "Display world coords");
  }

  @Override
  protected Align getDefaultAlignment() {return Align.BOTTOMRIGHT;}

  @Override
  protected int getDefaultOffsetX() {return 1;}

  @Override
  protected int getDefaultOffsetY() {return 1;}

  @Override
  protected double getDefaultScale() {return 1d;}

  @SubscribeEvent
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent ev) {
    if (getWorld() == null) return;

    LocalPlayer player = getLocalPlayer();
    thisX = player.getX();
    thisY = player.getY();
    thisZ = player.getZ();

    double thisFactor = getWorld().dimensionType().coordinateScale();
    double otherFactor = thisFactor != 1d ? 1d : 8d;
    double travelFactor = thisFactor / otherFactor;
    otherX = thisX * travelFactor;
    otherZ = thisZ * travelFactor;
  }

  @SubscribeEvent
  public void onRenderOverlay(RenderGuiEvent.Post event) {
    List<String> text = new ArrayList<>();

    if (!translate.get() || (translate.get() && multiline.get())) {
      text.add(String.format("%01.1f, %01.0f, %01.1f", thisX, thisY, thisZ));
    }
    if (translate.get()) {
      if (multiline.get()) {
        text.add(String.format("(%01.1f, %01.1f)", otherX, otherZ));
      } else {
        text.add(String.format(
            "%01.1f, %01.0f, %01.1f (%01.1f, %01.1f)", thisX, thisY, thisZ, otherX, otherZ));
      }
    }

    SurfaceHelper.setGraphics(event.getGuiGraphics());
    try {
      SurfaceHelper.drawTextAlign(
          text, getPosX(0), getPosY(0),
          Colors.WHITE.toBuffer(), scale.get(), true, alignment.get().ordinal()
      );
    } finally {
      SurfaceHelper.setGraphics(null);
    }
  }
}
