package com.matt.forgehax.mods;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceBuilder;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.math.Plane;
import com.matt.forgehax.util.math.VectorUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

import static com.matt.forgehax.Helper.getWorld;

@RegisterMod
public class ItemESP extends ToggleMod {
  public final Setting<Double> scale =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("scale")
          .description("Scaling for text")
          .defaultTo(1.D)
          .min(0.D)
          .build();
  private final List<ItemEntity> renderItems = new ArrayList<>();

  public ItemESP() {
    super(Category.RENDER, "ItemESP", false, "ESP for items");
  }

  @SubscribeEvent
  public void onUpdate(LocalPlayerUpdateEvent event) {
    renderItems.clear();
    for (net.minecraft.world.entity.Entity entity : getWorld().entitiesForRendering()) {
      if (entity instanceof ItemEntity && entity.tickCount > 1) {
        renderItems.add((ItemEntity) entity);
      }
    }
  }

  @Override
  protected void onDisabled() {
    renderItems.clear();
  }

  @SubscribeEvent
  public void onRender2D(final Render2DEvent event) {
    SurfaceBuilder.enableBlend();
    SurfaceBuilder.enableFontRendering(); // also disables depth test

    final double scale = this.scale.get() == 0 ? 1.D : this.scale.get();

    for (ItemEntity entity : renderItems) {
      Vec3 bottomPos = EntityUtils.getInterpolatedPos(entity, event.getPartialTicks());
      Vec3 topPos =
          bottomPos.add(0.D, entity.getBoundingBox().maxY - entity.getY(), 0.D);

      Plane top = VectorUtils.toScreen(topPos);
      Plane bot = VectorUtils.toScreen(bottomPos);

      if (!top.isVisible() && !bot.isVisible()) {
        continue;
      }

      ItemStack stack = entity.getItem();
      String text = stack.getHoverName().getString()
          + (stack.isStackable() ? (" x" + stack.getCount()) : "");

      double x = top.getX() - (SurfaceHelper.getTextWidth(text, scale) / 2.D);
      double y = top.getY() + (SurfaceHelper.getTextHeight(scale) / 2.D) - 1;

      SurfaceHelper.drawTextShadow(text, (int) x, (int) y, Colors.WHITE.toBuffer(), scale);
    }

    SurfaceBuilder.disableFontRendering();
    SurfaceBuilder.disableBlend();
  }
}
