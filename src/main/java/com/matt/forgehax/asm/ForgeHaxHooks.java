package com.matt.forgehax.asm;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.matt.forgehax.asm.TypesMc.Methods;
import com.matt.forgehax.asm.events.*;
import com.matt.forgehax.asm.events.EntityBlockSlipApplyEvent.Stage;
import com.matt.forgehax.asm.events.listeners.BlockModelRenderListener;
import com.matt.forgehax.asm.events.listeners.Listeners;
import com.matt.forgehax.asm.utils.MultiBoolean;
import com.matt.forgehax.asm.utils.debug.HookReporter;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ForgeHaxHooks implements ASMCommon {

  public static final Set<Class<? extends Block>> LIST_BLOCK_FILTER = Sets.newHashSet();
  public static final MultiBoolean SHOULD_DISABLE_CAVE_CULLING = new MultiBoolean();
  private static final List<HookReporter> ALL_REPORTERS = Lists.newArrayList();
  /**
   * onPushOutOfBlocks
   */
  public static final HookReporter HOOK_onPushOutOfBlocks =
      newHookReporter()
          .hook("onPushOutOfBlocks")
          .dependsOn(TypesMc.Methods.EntityPlayerSP_pushOutOfBlocks)
          .forgeEvent(PushOutOfBlocksEvent.class)
          .build();
  /**
   * onRenderBoat
   */
  public static final HookReporter HOOK_onRenderBoat =
      newHookReporter()
          .hook("onRenderBoat")
          .dependsOn(TypesMc.Methods.RenderBoat_doRender)
          .forgeEvent(RenderBoatEvent.class)
          .build();
  /**
   * onSchematicaPlaceBlock
   */
  public static final HookReporter HOOK_onSchematicaPlaceBlock =
      newHookReporter()
          .hook("onSchematicaPlaceBlock")
          .dependsOn(TypesSpecial.Methods.SchematicPrinter_placeBlock)
          .forgeEvent(SchematicaPlaceBlockEvent.class)
          .build();
  /**
   * onHurtcamEffect
   */
  public static final HookReporter HOOK_onHurtcamEffect =
      newHookReporter()
          .hook("onHurtcamEffect")
          .dependsOn(TypesMc.Methods.EntityRenderer_hurtCameraEffect)
          .forgeEvent(HurtCamEffectEvent.class)
          .build();
  /**
   * onSendingPacket
   */
  public static final HookReporter HOOK_onSendingPacket =
      newHookReporter()
          .hook("onSendingPacket")
          .dependsOn(TypesMc.Methods.NetworkManager_dispatchPacket)
          .dependsOn(TypesMc.Methods.NetworkManager$4_run)
          .forgeEvent(PacketEvent.Outgoing.Pre.class)
          .build();
  /**
   * onSentPacket
   */
  public static final HookReporter HOOK_onSentPacket =
      newHookReporter()
          .hook("onSentPacket")
          .dependsOn(TypesMc.Methods.NetworkManager_dispatchPacket)
          .dependsOn(TypesMc.Methods.NetworkManager$4_run)
          .forgeEvent(PacketEvent.Outgoing.Post.class)
          .build();
  /**
   * onPreReceived
   */
  public static final HookReporter HOOK_onPreReceived =
      newHookReporter()
          .hook("onPreReceived")
          .dependsOn(TypesMc.Methods.NetworkManager_channelRead0)
          .forgeEvent(PacketEvent.Incoming.Pre.class)
          .build();

  /** static hooks */
  /**
   * onPostReceived
   */
  public static final HookReporter HOOK_onPostReceived =
      newHookReporter()
          .hook("onPostReceived")
          .dependsOn(TypesMc.Methods.NetworkManager_channelRead0)
          .forgeEvent(PacketEvent.Incoming.Post.class)
          .build();
  /**
   * onWaterMovement
   */
  public static final HookReporter HOOK_onWaterMovement =
      newHookReporter()
          .hook("onWaterMovement")
          .dependsOn(TypesMc.Methods.World_handleMaterialAcceleration)
          .forgeEvent(WaterMovementEvent.class)
          .build();
  /**
   * onApplyCollisionMotion
   */
  public static final HookReporter HOOK_onApplyCollisionMotion =
      newHookReporter()
          .hook("onApplyCollisionMotion")
          .dependsOn(TypesMc.Methods.Entity_applyEntityCollision)
          .forgeEvent(ApplyCollisionMotionEvent.class)
          .build();
  /**
   * onPutColorMultiplier
   */
  public static final HookReporter HOOK_onPutColorMultiplier =
      newHookReporter()
          .hook("onPutColorMultiplier")
          .dependsOn(TypesMc.Methods.BufferBuilder_putColorMultiplier)
          .build();
  /**
   * onPreRenderBlockLayer
   */
  public static final HookReporter HOOK_onPreRenderBlockLayer =
      newHookReporter()
          .hook("onPreRenderBlockLayer")
          .dependsOn(TypesMc.Methods.RenderGlobal_renderBlockLayer)
          .forgeEvent(RenderBlockLayerEvent.Pre.class)
          .build();
  /**
   * onPostRenderBlockLayer
   */
  public static final HookReporter HOOK_onPostRenderBlockLayer =
      newHookReporter()
          .hook("onPostRenderBlockLayer")
          .dependsOn(TypesMc.Methods.RenderGlobal_renderBlockLayer)
          .forgeEvent(RenderBlockLayerEvent.Post.class)
          .build();
  /**
   * onSetupTerrain
   */
  public static final HookReporter HOOK_onSetupTerrain =
      newHookReporter()
          .hook("onSetupTerrain")
          .dependsOn(TypesMc.Methods.RenderGlobal_setupTerrain)
          .forgeEvent(SetupTerrainEvent.class)
          .build();
  /**
   * onComputeVisibility
   */
  public static final HookReporter HOOK_onComputeVisibility =
      newHookReporter()
          .hook("onComputeVisibility")
          // no hook exists anymore
          .forgeEvent(ComputeVisibilityEvent.class)
          .build();
  /**
   * onDoBlockCollisions
   */
  public static final HookReporter HOOK_onDoBlockCollisions =
      newHookReporter()
          .hook("onDoBlockCollisions")
          // no hook exists anymore
          .forgeEvent(DoBlockCollisionsEvent.class)
          .build();
  /**
   * isBlockFiltered
   */
  public static final HookReporter HOOK_isBlockFiltered =
      newHookReporter()
          .hook("isBlockFiltered")
          .dependsOn(TypesMc.Methods.Entity_doBlockCollisions)
          .build();
  /**
   * onApplyClimbableBlockMovement
   */
  public static final HookReporter HOOK_onApplyClimbableBlockMovement =
      newHookReporter()
          .hook("onApplyClimbableBlockMovement")
          // no hook exists
          .forgeEvent(ApplyClimbableBlockMovement.class)
          .build();
  /**
   * onRenderBlockInLayer
   */
  public static final HookReporter HOOK_onRenderBlockInLayer =
      newHookReporter()
          .hook("onRenderBlockInLayer")
          .dependsOn(TypesMc.Methods.Block_canRenderInLayer)
          .forgeEvent(RenderBlockInLayerEvent.class)
          .build();
  /**
   * onBlockRender
   */
  public static final HookReporter HOOK_onBlockRender =
      newHookReporter()
          .hook("onBlockRender")
          // no hook exists
          .forgeEvent(BlockRenderEvent.class)
          .build();
  /**
   * onAddCollisionBoxToList
   */
  public static final HookReporter HOOK_onAddCollisionBoxToList =
      newHookReporter()
          .hook("onAddCollisionBoxToList")
          .dependsOn(TypesMc.Methods.Block_addCollisionBoxToList)
          .forgeEvent(AddCollisionBoxToListEvent.class)
          .build();
  /**
   * onBlockRenderInLoop
   */
  public static final HookReporter HOOK_onBlockRenderInLoop =
      newHookReporter()
          .hook("onBlockRenderInLoop")
          .dependsOn(TypesMc.Methods.RenderChunk_rebuildChunk)
          .listenerEvent(BlockModelRenderListener.class)
          .build();
  /**
   * onPreBuildChunk
   */
  public static final HookReporter HOOK_onPreBuildChunk =
      newHookReporter()
          .hook("onPreBuildChunk")
          .dependsOn(TypesMc.Methods.RenderChunk_rebuildChunk)
          .forgeEvent(BuildChunkEvent.Pre.class)
          .build();
  /**
   * onPostBuildChunk
   */
  public static final HookReporter HOOK_onPostBuildChunk =
      newHookReporter()
          .hook("onPostBuildChunk")
          .dependsOn(TypesMc.Methods.RenderChunk_rebuildChunk)
          .forgeEvent(BuildChunkEvent.Post.class)
          .build();
  /**
   * onDeleteGlResources
   */
  public static final HookReporter HOOK_onDeleteGlResources =
      newHookReporter()
          .hook("onDeleteGlResources")
          .dependsOn(TypesMc.Methods.RenderChunk_deleteGlResources)
          .forgeEvent(DeleteGlResourcesEvent.class)
          .build();
  /**
   * onAddRenderChunk
   */
  public static final HookReporter HOOK_onAddRenderChunk =
      newHookReporter()
          .hook("onAddRenderChunk")
          .dependsOn(TypesMc.Methods.ChunkRenderContainer_addRenderChunk)
          .forgeEvent(AddRenderChunkEvent.class)
          .build();
  /**
   * onChunkUploaded
   */
  public static final HookReporter HOOK_onChunkUploaded =
      newHookReporter()
          .hook("onChunkUploaded")
          .dependsOn(TypesMc.Methods.ChunkRenderDispatcher_uploadChunk)
          .forgeEvent(ChunkUploadedEvent.class)
          .build();
  /**
   * onLoadRenderers
   */
  public static final HookReporter HOOK_onLoadRenderers =
      newHookReporter()
          .hook("onLoadRenderers")
          .dependsOn(TypesMc.Methods.RenderGlobal_loadRenderers)
          .forgeEvent(LoadRenderersEvent.class)
          .build();
  /**
   * onWorldRendererDeallocated
   */
  public static final HookReporter HOOK_onWorldRendererDeallocated =
      newHookReporter()
          .hook("onWorldRendererDeallocated")
          .dependsOn(TypesMc.Methods.ChunkRenderWorker_freeRenderBuilder)
          .forgeEvent(WorldRendererDeallocatedEvent.class)
          .build();
  /**
   * shouldDisableCaveCulling
   */
  public static final HookReporter HOOK_shouldDisableCaveCulling =
      newHookReporter()
          .hook("shouldDisableCaveCulling")
          .dependsOn(TypesMc.Methods.RenderGlobal_setupTerrain)
          .dependsOn(TypesMc.Methods.VisGraph_setOpaqueCube)
          .dependsOn(TypesMc.Methods.VisGraph_computeVisibility)
          .build();
  /**
   * onUpdateWalkingPlayerPre
   */
  public static final HookReporter HOOK_onUpdateWalkingPlayerPre =
      newHookReporter()
          .hook("onUpdateWalkingPlayerPre")
          .dependsOn(TypesMc.Methods.EntityPlayerSP_onUpdateWalkingPlayer)
          .forgeEvent(LocalPlayerUpdateMovementEvent.Pre.class)
          .build();
  /**
   * onUpdateWalkingPlayerPost
   */
  public static final HookReporter HOOK_onUpdateWalkingPlayerPost =
      newHookReporter()
          .hook("onUpdateWalkingPlayerPost")
          .dependsOn(TypesMc.Methods.EntityPlayerSP_onUpdateWalkingPlayer)
          .forgeEvent(LocalPlayerUpdateMovementEvent.Post.class)
          .build();
  /**
   * onWorldCheckLightFor
   */
  public static final HookReporter HOOK_onWorldCheckLightFor =
      newHookReporter()
          .hook("onWorldCheckLightFor")
          .dependsOn(TypesMc.Methods.World_checkLightFor)
          .forgeEvent(WorldCheckLightForEvent.class)
          .build();
  /**
   * onLeftClickCounterSet
   */
  public static final HookReporter HOOK_onLeftClickCounterSet =
      newHookReporter()
          .hook("onLeftClickCounterSet")
          .dependsOn(TypesMc.Methods.Minecraft_runTick)
          .dependsOn(TypesMc.Methods.Minecraft_setIngameFocus)
          .forgeEvent(LeftClickCounterUpdateEvent.class)
          .build();
  /**
   * onSendClickBlockToController
   */
  public static final HookReporter HOOK_onSendClickBlockToController =
      newHookReporter()
          .hook("onSendClickBlockToController")
          .dependsOn(TypesMc.Methods.Minecraft_runTick)
          .forgeEvent(BlockControllerProcessEvent.class)
          .build();
  /**
   * onPlayerItemSync
   */
  public static final HookReporter HOOK_onPlayerItemSync =
      newHookReporter()
          .hook("onPlayerItemSync")
          .dependsOn(Methods.PlayerControllerMC_syncCurrentPlayItem)
          .forgeEvent(PlayerSyncItemEvent.class)
          .build();
  /**
   * onPlayerBreakingBlock
   */
  public static final HookReporter HOOK_onPlayerBreakingBlock =
      newHookReporter()
          .hook("onPlayerBreakingBlock")
          .dependsOn(Methods.PlayerControllerMC_onPlayerDamageBlock)
          .forgeEvent(PlayerDamageBlockEvent.class)
          .build();
  /**
   * onPlayerAttackEntity
   */
  public static final HookReporter HOOK_onPlayerAttackEntity =
      newHookReporter()
          .hook("onPlayerAttackEntity")
          .dependsOn(Methods.PlayerControllerMC_attackEntity)
          .forgeEvent(PlayerAttackEntityEvent.class)
          .build();
  /**
   * onPlayerStopUse
   */
  public static final HookReporter HOOK_onPlayerStopUse =
      newHookReporter()
          .hook("onPlayerStopUse")
          .dependsOn(Methods.PlayerControllerMC_onStoppedUsingItem)
          .forgeEvent(ItemStoppedUsedEvent.class)
          .build();
  /**
   * onPlayerStopUse
   */
  public static final HookReporter HOOK_onEntityBlockSlipApply =
      newHookReporter()
          .hook("onEntityBlockSlipApply")
          .dependsOn(Methods.PlayerControllerMC_onStoppedUsingItem)
          .forgeEvent(EntityBlockSlipApplyEvent.class)
          .build();
  /**
   * static fields
   */
  public static boolean isSafeWalkActivated = false;
  public static boolean isNoSlowDownActivated = false;
  public static boolean isNoBoatGravityActivated = false;
  public static boolean isNoClampingActivated = false;
  public static boolean isBoatSetYawActivated = false;
  public static boolean isNotRowingBoatActivated = false;
  public static boolean doIncreaseTabListSize = false;
  public static boolean SHOULD_UPDATE_ALPHA = false;
  public static float COLOR_MULTIPLIER_ALPHA = 150.f / 255.f;

  public static List<HookReporter> getReporters() {
    return Collections.unmodifiableList(ALL_REPORTERS);
  }

  private static HookReporter.Builder newHookReporter() {
    return HookReporter.Builder.of()
                               .parentClass(ForgeHaxHooks.class)
                               .finalizeBy(ALL_REPORTERS::add);
  }

  /**
   * Convenient functions for firing events
   */
  public static void fireEvent_v(Event event) {
    MinecraftForge.EVENT_BUS.post(event);
  }

  public static boolean fireEvent_b(Event event) {
    return MinecraftForge.EVENT_BUS.post(event);
  }

  /**
   * onDrawBoundingBox
   */
  public static void onDrawBoundingBoxPost() {
    MinecraftForge.EVENT_BUS.post(new DrawBlockBoundingBoxEvent.Post());
  }

  public static boolean onPushOutOfBlocks() {
    return HOOK_onPushOutOfBlocks.reportHook()
        && MinecraftForge.EVENT_BUS.post(new PushOutOfBlocksEvent());
  }

  public static float onRenderBoat(Boat boat, float entityYaw) {
    if (HOOK_onRenderBoat.reportHook()) {
      RenderBoatEvent event = new RenderBoatEvent(boat, entityYaw);
      MinecraftForge.EVENT_BUS.post(event);
      return event.getYaw();
    } else {
      return entityYaw;
    }
  }

  public static void onSchematicaPlaceBlock(ItemStack itemIn, BlockPos posIn, Vec3 vecIn, Direction sideIn) {
    if (HOOK_onSchematicaPlaceBlock.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new SchematicaPlaceBlockEvent(itemIn, posIn, vecIn, sideIn));
    }
  }

  public static boolean onHurtcamEffect(float partialTicks) {
    return HOOK_onHurtcamEffect.reportHook()
        && MinecraftForge.EVENT_BUS.post(new HurtCamEffectEvent(partialTicks));
  }

  public static boolean onSendingPacket(Packet<?> packet) {
    return HOOK_onSendingPacket.reportHook()
        && MinecraftForge.EVENT_BUS.post(new PacketEvent.Outgoing.Pre(packet));
  }

  public static void onSentPacket(Packet<?> packet) {
    if (HOOK_onSentPacket.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new PacketEvent.Outgoing.Post(packet));
    }
  }

  public static boolean onPreReceived(Packet<?> packet) {
    return HOOK_onPreReceived.reportHook()
        && MinecraftForge.EVENT_BUS.post(new PacketEvent.Incoming.Pre(packet));
  }

  public static void onPostReceived(Packet<?> packet) {
    if (HOOK_onPostReceived.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new PacketEvent.Incoming.Post(packet));
    }
  }

  public static boolean onWaterMovement(Entity entity, Vec3 moveDir) {
    return HOOK_onWaterMovement.reportHook()
        && MinecraftForge.EVENT_BUS.post(new WaterMovementEvent(entity, moveDir));
  }

  public static boolean onApplyCollisionMotion(
      Entity entity, Entity collidedWithEntity, double x, double z) {
    return HOOK_onApplyCollisionMotion.reportHook()
        && MinecraftForge.EVENT_BUS.post(
        new ApplyCollisionMotionEvent(entity, collidedWithEntity, x, 0.D, z));
  }

  public static int onPutColorMultiplier(float r, float g, float b, int buffer, boolean[] flag) {
    flag[0] = SHOULD_UPDATE_ALPHA;
    if (HOOK_onPutColorMultiplier.reportHook() && SHOULD_UPDATE_ALPHA) {
      if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
        int red = (int) ((float) (buffer & 255) * r);
        int green = (int) ((float) (buffer >> 8 & 255) * g);
        int blue = (int) ((float) (buffer >> 16 & 255) * b);
        int alpha = (int) (((float) (buffer >> 24 & 255) * COLOR_MULTIPLIER_ALPHA));
        buffer = alpha << 24 | blue << 16 | green << 8 | red;
      } else {
        int red = (int) ((float) (buffer >> 24 & 255) * r);
        int green = (int) ((float) (buffer >> 16 & 255) * g);
        int blue = (int) ((float) (buffer >> 8 & 255) * b);
        int alpha = (int) (((float) (buffer & 255) * COLOR_MULTIPLIER_ALPHA));
        buffer = red << 24 | green << 16 | blue << 8 | alpha;
      }
    }
    return buffer;
  }

  public static boolean onPreRenderBlockLayer(RenderType layer, double partialTicks) {
    return HOOK_onPreRenderBlockLayer.reportHook()
        && MinecraftForge.EVENT_BUS.post(new RenderBlockLayerEvent.Pre(layer, partialTicks));
  }

  public static void onPostRenderBlockLayer(RenderType layer, double partialTicks) {
    if (HOOK_onPostRenderBlockLayer.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new RenderBlockLayerEvent.Post(layer, partialTicks));
    }
  }

  public static boolean onSetupTerrain(Entity renderEntity, boolean playerSpectator) {
    if (HOOK_onSetupTerrain.reportHook()) {
      SetupTerrainEvent event = new SetupTerrainEvent(renderEntity, playerSpectator);
      MinecraftForge.EVENT_BUS.post(event);
      return event.isCulling();
    } else {
      return playerSpectator;
    }
  }

  @Deprecated
  public static void onComputeVisibility(VisGraph visGraph, VisibilitySet setVisibility) {
    if (HOOK_onComputeVisibility.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new ComputeVisibilityEvent(visGraph, setVisibility));
    }
  }

  @Deprecated
  public static boolean onDoBlockCollisions(Entity entity, BlockPos pos, BlockState state) {
    return HOOK_onDoBlockCollisions.reportHook()
        && MinecraftForge.EVENT_BUS.post(new DoBlockCollisionsEvent(entity, pos, state));
  }

  public static boolean isBlockFiltered(Entity entity, BlockState state) {
    return HOOK_isBlockFiltered.reportHook()
        && entity instanceof Player
        && LIST_BLOCK_FILTER.contains(state.getBlock().getClass());
  }

  @Deprecated
  public static boolean onApplyClimbableBlockMovement(LivingEntity livingBase) {
    return HOOK_onApplyClimbableBlockMovement.reportHook()
        && MinecraftForge.EVENT_BUS.post(new ApplyClimbableBlockMovement(livingBase));
  }

  public static RenderType onRenderBlockInLayer(
      Block block, BlockState state, RenderType layer, RenderType compareToLayer) {
    if (HOOK_onRenderBlockInLayer.reportHook()) {
      RenderBlockInLayerEvent event =
          new RenderBlockInLayerEvent(block, state, layer, compareToLayer);
      MinecraftForge.EVENT_BUS.post(event);
      return event.getLayer();
    } else {
      return layer;
    }
  }

  @Deprecated
  public static void onBlockRender(
      BlockPos pos, BlockState state, BlockAndTintGetter access, BufferBuilder buffer) {
    if (HOOK_onBlockRender.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new BlockRenderEvent(pos, state, access, buffer));
    }
  }

  /**
   * Collision shapes are resolved for every entity's every move, on the client and the integrated
   * server both. Only the local player and whatever it is riding can matter to a client mod, so
   * everything else is rejected before the event is built.
   */
  public static boolean shouldFireCollisionBoxes(Entity entity) {
    if (entity == null || !HOOK_onAddCollisionBoxToList.reportHook()) {
      return false;
    }
    LocalPlayer player = Minecraft.getInstance().player;
    return player != null && (entity == player || entity == player.getVehicle());
  }

  public static boolean onAddCollisionBoxToList(
      Block block,
      BlockState state,
      Level worldIn,
      BlockPos pos,
      AABB entityBox,
      List<AABB> collidingBoxes,
      Entity entityIn,
      boolean bool
  ) {
    return HOOK_onAddCollisionBoxToList.reportHook()
        && MinecraftForge.EVENT_BUS.post(
        new AddCollisionBoxToListEvent(
            block, state, worldIn, pos, entityBox, collidingBoxes, entityIn, bool));
  }

  public static void onBlockRenderInLoop(
      RenderChunk renderChunk, Block block, BlockState state, BlockPos pos) {
    // faster hook
    if (HOOK_onBlockRenderInLoop.reportHook()) {
      for (BlockModelRenderListener listener : Listeners.BLOCK_MODEL_RENDER_LISTENER.getAll()) {
        listener.onBlockRenderInLoop(renderChunk, block, state, pos);
      }
    }
  }

  public static void onPreBuildChunk(RenderChunk renderChunk) {
    if (HOOK_onPreBuildChunk.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new BuildChunkEvent.Pre(renderChunk));
    }
  }

  public static void onPostBuildChunk(RenderChunk renderChunk) {
    // i couldn't place a post block render hook within the if label so I have to do this
    if (HOOK_onPostBuildChunk.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new BuildChunkEvent.Post(renderChunk));
    }
  }

  public static void onDeleteGlResources(RenderChunk renderChunk) {
    if (HOOK_onDeleteGlResources.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new DeleteGlResourcesEvent(renderChunk));
    }
  }

  public static void onAddRenderChunk(RenderChunk renderChunk, RenderType layer) {
    if (HOOK_onAddRenderChunk.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new AddRenderChunkEvent(renderChunk, layer));
    }
  }

  public static void onChunkUploaded(RenderChunk chunk, BufferBuilder.RenderedBuffer buffer) {
    if (HOOK_onChunkUploaded.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new ChunkUploadedEvent(chunk, buffer));
    }
  }

  public static void onLoadRenderers(
      ViewArea viewFrustum, ChunkRenderDispatcher renderDispatcher) {
    if (HOOK_onLoadRenderers.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new LoadRenderersEvent(viewFrustum, renderDispatcher));
    }
  }

  public static void onWorldRendererDeallocated(Object generator, RenderChunk renderChunk) {
    if (HOOK_onWorldRendererDeallocated.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new WorldRendererDeallocatedEvent(generator, renderChunk));
    }
  }

  public static boolean shouldDisableCaveCulling() {
    return HOOK_shouldDisableCaveCulling.reportHook() && SHOULD_DISABLE_CAVE_CULLING.isEnabled();
  }

  public static boolean onUpdateWalkingPlayerPre(LocalPlayer localPlayer) {
    return HOOK_onUpdateWalkingPlayerPre.reportHook()
        && MinecraftForge.EVENT_BUS.post(new LocalPlayerUpdateMovementEvent.Pre(localPlayer));
  }

  public static void onUpdateWalkingPlayerPost(LocalPlayer localPlayer) {
    if (HOOK_onUpdateWalkingPlayerPost.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new LocalPlayerUpdateMovementEvent.Post(localPlayer));
    }
  }

  public static boolean onWorldCheckLightFor(LightLayer enumSkyBlock, BlockPos pos) {
    return HOOK_onWorldCheckLightFor.reportHook()
        && MinecraftForge.EVENT_BUS.post(new WorldCheckLightForEvent(enumSkyBlock, pos));
  }

  public static int onLeftClickCounterSet(int value, Minecraft minecraft) {
    if (HOOK_onLeftClickCounterSet.reportHook()) {
      LeftClickCounterUpdateEvent event = new LeftClickCounterUpdateEvent(minecraft, value);
      return MinecraftForge.EVENT_BUS.post(event) ? event.getCurrentValue() : event.getValue();
    } else {
      return value;
    }
  }

  public static boolean onSendClickBlockToController(Minecraft minecraft, boolean clicked) {
    if (HOOK_onSendClickBlockToController.reportHook()) {
      BlockControllerProcessEvent event = new BlockControllerProcessEvent(minecraft, clicked);
      MinecraftForge.EVENT_BUS.post(event);
      return event.isLeftClicked();
    } else {
      return clicked;
    }
  }

  public static void onPlayerItemSync(MultiPlayerGameMode playerControllerMP) {
    if (HOOK_onPlayerItemSync.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new PlayerSyncItemEvent(playerControllerMP));
    }
  }

  public static void onPlayerBreakingBlock(
      MultiPlayerGameMode playerControllerMP, BlockPos pos, Direction facing) {
    if (HOOK_onPlayerBreakingBlock.reportHook()) {
      MinecraftForge.EVENT_BUS.post(new PlayerDamageBlockEvent(playerControllerMP, pos, facing));
    }
  }

  public static void onPlayerAttackEntity(
      MultiPlayerGameMode playerControllerMP, Player attacker, Entity victim) {
    if (HOOK_onPlayerAttackEntity.reportHook()) {
      MinecraftForge.EVENT_BUS.post(
          new PlayerAttackEntityEvent(playerControllerMP, attacker, victim));
    }
  }

  public static boolean onPlayerStopUse(
      MultiPlayerGameMode playerControllerMP, Player player) {
    return HOOK_onPlayerStopUse.reportHook()
        && MinecraftForge.EVENT_BUS.post(new ItemStoppedUsedEvent(playerControllerMP, player));
  }

  public static float onEntityBlockSlipApply(
      float defaultSlipperiness,
      LivingEntity entityLivingBase,
      BlockState blockStateUnder,
      int stage
  ) {
    if (HOOK_onEntityBlockSlipApply.reportHook()) {
      EntityBlockSlipApplyEvent event =
          new EntityBlockSlipApplyEvent(
              Stage.values()[stage], entityLivingBase, blockStateUnder, defaultSlipperiness);
      MinecraftForge.EVENT_BUS.post(event);
      return event.getSlipperiness();
    } else {
      return defaultSlipperiness;
    }
  }
}
