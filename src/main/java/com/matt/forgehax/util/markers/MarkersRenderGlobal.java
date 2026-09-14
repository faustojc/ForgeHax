package com.matt.forgehax.util.markers;

import com.matt.forgehax.Globals;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Created on 7/26/2017 by fr1kin
 *
 * 1.12.2 -> 1.20.1: {@code RenderGlobal} was rewritten and renamed to {@link LevelRenderer}.
 * Chunk/terrain/entity rendering that used to live in overridable public methods is now
 * private and folded into the giant {@code renderLevel(...)} method, so most of the old
 * override points below have no equivalent left to override - they were removed rather than
 * ported. Methods kept here are genuine overrides (renamed to the current LevelRenderer API);
 * everything that needs a real hook now requires a mixin. See the TODO list at the bottom of
 * this file for what a later phase must add.
 */
public class MarkersRenderGlobal extends LevelRenderer implements Globals {

  private static final MarkersRenderGlobal INSTANCE = new MarkersRenderGlobal(MC);

  public MarkersRenderGlobal(Minecraft mcIn) {
    super(mcIn, mcIn.getEntityRenderDispatcher(), mcIn.getBlockEntityRenderDispatcher(), mcIn.renderBuffers());
  }

  public static MarkersRenderGlobal getInstance() {
    return INSTANCE;
  }

  @Override
  public void onResourceManagerReload(ResourceManager resourceManager) {
    super.onResourceManagerReload(resourceManager);
  }

  /** Was renderEntityOutlineFramebuffer() in 1.12.2. */
  @Override
  public void doEntityOutline() {
    super.doEntityOutline();
  }

  /** Was protected isRenderEntityOutlines() in 1.12.2; now public. */
  @Override
  public boolean shouldShowEntityOutlines() {
    return super.shouldShowEntityOutlines();
  }

  /** Was setWorldAndLoadRenderers(WorldClient) in 1.12.2. */
  @Override
  public void setLevel(@Nullable ClientLevel worldClientIn) {
    super.setLevel(worldClientIn);
  }

  /** Was loadRenderers() in 1.12.2. */
  @Override
  public void allChanged() {
    super.allChanged();
  }

  /** Was getDebugInfoRenders() in 1.12.2. */
  @Override
  public String getChunkStatistics() {
    return super.getChunkStatistics();
  }

  /** Was protected getRenderedChunks() in 1.12.2; now public. */
  @Override
  public int countRenderedChunks() {
    return super.countRenderedChunks();
  }

  /** Was getDebugInfoEntities() in 1.12.2. */
  @Override
  public String getEntityStatistics() {
    return super.getEntityStatistics();
  }

  /** Signature changed (PoseStack/Matrix4f/Camera/Runnable replace the old float/int params). */
  @Override
  public void renderSky(
      PoseStack poseStack,
      Matrix4f projectionMatrix,
      float partialTicks,
      Camera camera,
      boolean isFoggy,
      Runnable skyFogSetup
  ) {
    super.renderSky(poseStack, projectionMatrix, partialTicks, camera, isFoggy, skyFogSetup);
  }

  /** Signature changed (PoseStack/Matrix4f replace the old float/int/double params). */
  @Override
  public void renderClouds(
      PoseStack poseStack,
      Matrix4f projectionMatrix,
      float partialTicks,
      double camX,
      double camY,
      double camZ
  ) {
    super.renderClouds(poseStack, projectionMatrix, partialTicks, camX, camY, camZ);
  }

  /** Was notifyBlockUpdate(World, BlockPos, IBlockState, IBlockState, int); World -> BlockGetter. */
  @Override
  public void blockChanged(
      BlockGetter worldIn, BlockPos pos, BlockState oldState, BlockState newState, int flags) {
    super.blockChanged(worldIn, pos, oldState, newState, flags);
  }

  /**
   * Was the custom (non-override) markBlockRangeForRenderUpdate(...) stub in 1.12.2; in 1.20.1
   * setBlocksDirty(...) has the identical signature and is a real override.
   */
  @Override
  public void setBlocksDirty(int x1, int y1, int z1, int x2, int y2, int z2) {
    super.setBlocksDirty(x1, y1, z1, x2, y2, z2);
  }

  /** Was playRecord(SoundEvent, BlockPos) in 1.12.2. */
  @Override
  public void playStreamingMusic(@Nullable SoundEvent soundIn, BlockPos pos) {
    super.playStreamingMusic(soundIn, pos);
  }

  @Override
  public void playStreamingMusic(
      @Nullable SoundEvent soundIn, BlockPos pos, @Nullable RecordItem musicDiscItem) {
    super.playStreamingMusic(soundIn, pos, musicDiscItem);
  }

  /** Was broadcastSound(int, BlockPos, int) in 1.12.2. */
  @Override
  public void globalLevelEvent(int soundID, BlockPos pos, int data) {
    super.globalLevelEvent(soundID, pos, data);
  }

  /** Was playEvent(EntityPlayer, int, BlockPos, int) in 1.12.2; the player param was dropped. */
  @Override
  public void levelEvent(int type, BlockPos blockPosIn, int data) {
    super.levelEvent(type, blockPosIn, data);
  }

  /** Was sendBlockBreakProgress(int, BlockPos, int) in 1.12.2. */
  @Override
  public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {
    super.destroyBlockProgress(breakerId, pos, progress);
  }

  /** Was hasNoChunkUpdates() in 1.12.2 (inverse-sounding name, same override point). */
  @Override
  public boolean hasRenderedAllChunks() {
    return super.hasRenderedAllChunks();
  }

  /** Was setDisplayListEntitiesDirty() in 1.12.2. */
  @Override
  public void needsUpdate() {
    super.needsUpdate();
  }

  /** Was updateTileEntities(Collection<TileEntity>, Collection<TileEntity>) in 1.12.2. */
  @Override
  public void updateGlobalBlockEntities(
      Collection<BlockEntity> tileEntitiesToRemove, Collection<BlockEntity> tileEntitiesToAdd) {
    super.updateGlobalBlockEntities(tileEntitiesToRemove, tileEntitiesToAdd);
  }

  // TODO(1.20.1): the following 1.12.2 RenderGlobal override points have no equivalent public/
  // protected method left on LevelRenderer to override - the logic was folded into private
  // methods (mostly the giant renderLevel(...)). Actually hooking these requires a mixin into
  // LevelRenderer (or ChunkRenderDispatcher for the chunk-compile ones) rather than a subclass
  // override. Left out of this class entirely; a later phase adds the mixins:
  //   - stopChunkUpdates()                              -> chunk update queue is now private
  //   - createBindEntityOutlineFbs(int, int)             -> entity outline RTs sized internally
  //                                                          by initOutline()/resize(int,int)
  //   - renderEntities(Entity, ICamera, float)           -> folded into renderLevel(...)
  //   - setupTerrain(Entity, double, ICamera, int, bool) -> replaced by private setupRender(...)
  //   - renderBlockLayer(BlockRenderLayer, double, int, Entity)
  //                                                       -> private renderChunkLayer(RenderType, ...)
  //   - updateClouds()                                   -> cloud geometry rebuilt inline in
  //                                                          renderClouds(...)
  //   - hasCloudFog(double, double, double, float)        -> no equivalent found on LevelRenderer
  //   - updateChunks(long)                               -> replaced by private compileChunks(Camera)
  //   - renderWorldBorder(Entity, float)                 -> private renderWorldBorder(Camera)
  //   - drawBlockDamageTexture(Tessellator, BufferBuilder, Entity, float)
  //                                                       -> folded into renderLevel(...)
  //   - drawSelectionBox(EntityPlayer, RayTraceResult, int, float)
  //                                                       -> private renderHitOutline(...)
  //   - notifyLightSet(BlockPos)                         -> no equivalent found on LevelRenderer
  //   - playSoundToAllNearExcept(EntityPlayer, SoundEvent, SoundCategory, double, double, double,
  //     float, float)                                    -> moved off LevelRenderer entirely (now
  //                                                          on SoundManager)
  //   - spawnParticle(int id, boolean, boolean, double..., int...)
  //                                                       -> addParticle(ParticleOptions, ...) uses
  //                                                          a completely different particle API
  //   - onEntityAdded(Entity) / onEntityRemoved(Entity)  -> no equivalent found on LevelRenderer
  //   - deleteAllDisplayLists()                          -> display lists don't exist anymore,
  //                                                          concept is obsolete
}
