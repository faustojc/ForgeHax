package com.matt.forgehax.mods;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.asm.events.RenderBlockInLayerEvent;
import com.matt.forgehax.asm.events.RenderBlockLayerEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.matt.forgehax.Helper.reloadChunks;

@RegisterMod
public class XrayMod extends ToggleMod {

  public final Setting<Integer> opacity =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("opacity")
          .description("Xray opacity")
          .defaultTo(150)
          .min(0)
          .max(255)
          .changed(
              cb -> {
                ForgeHaxHooks.COLOR_MULTIPLIER_ALPHA = (cb.getTo().floatValue() / 255.f);
                reloadChunks();
              })
          .build();

  public XrayMod() {
    super(Category.WORLD, "Xray", false, "See blocks through walls");
  }

  @Override
  public void onEnabled() {
    // TODO(1.20.1): ForgeModContainer.forgeLightPipelineEnabled had no 1.20.1 equivalent
    // (the "fancy" forge lighting pipeline toggle was a 1.12 feature); dropped.
    ForgeHaxHooks.COLOR_MULTIPLIER_ALPHA = (this.opacity.getAsFloat() / 255.f);
    ForgeHaxHooks.SHOULD_UPDATE_ALPHA = true;
    reloadChunks();
    ForgeHaxHooks.SHOULD_DISABLE_CAVE_CULLING.enable("Xray");
  }

  @Override
  public void onDisabled() {
    ForgeHaxHooks.SHOULD_UPDATE_ALPHA = false;
    reloadChunks();
    ForgeHaxHooks.SHOULD_DISABLE_CAVE_CULLING.disable("Xray");
  }

  // TODO(1.20.1): the old TextureMap blur/mipmap dance around the layer re-render still needs
  // porting to TextureAtlas/AbstractTexture; the hook itself now fires (MixinLevelRenderer).
  @SubscribeEvent
  public void onPreRenderBlockLayer(RenderBlockLayerEvent.Pre event) {
    if (!event.getRenderLayer().equals(RenderType.translucent())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onPostRenderBlockLayer(RenderBlockLayerEvent.Post event) {
  }

  // 1.20.1 resolves one chunk layer per state, so layer and compare-to layer are the same value
  // (see MixinItemBlockRenderTypes); the per-layer canRenderInLayer query is gone.
  @SubscribeEvent
  public void onRenderBlockInLayer(RenderBlockInLayerEvent event) {
    if (event.getCompareToLayer().equals(RenderType.translucent())) {
      event.setLayer(event.getCompareToLayer());
    }
  }
}
