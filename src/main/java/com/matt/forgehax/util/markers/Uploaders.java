package com.matt.forgehax.util.markers;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created on 1/18/2018 by fr1kin
 */
public class Uploaders<E extends Tesselator> {

  private final Map<ChunkRenderDispatcher.RenderChunk, RenderUploader<E>> uploaders =
      Maps.newConcurrentMap();
  private final UploaderSupplier<E> supplier;

  private final TessellatorCache<E> cache;

  private Consumer<RenderUploader<E>> shutdownTask;

  public Uploaders(UploaderSupplier<E> supplier, TessellatorCache<E> cache) {
    this.supplier = supplier;
    this.cache = cache;
  }

  public static boolean isDummy(ChunkRenderDispatcher.RenderChunk chunk) {
    return chunk != null
        && chunk.getCompiledChunk() == ChunkRenderDispatcher.CompiledChunk.UNCOMPILED;
  }

  /**
   * Register RenderChunk and create new RenderUploader instance for it
   */
  public void register(ChunkRenderDispatcher.RenderChunk renderChunk) {
    RenderUploader<E> uploader = uploaders.get(renderChunk);
    // if a key for this object already exists, notify the shutdown hook and remove the old entry
    if (uploader != null && shutdownTask != null) {
      shutdownTask.accept(uploader);
    }
    uploaders.put(renderChunk, supplier.get(this));
  }

  /**
   * Unregister RenderChunk
   */
  public void unregister(ChunkRenderDispatcher.RenderChunk renderChunk) {
    RenderUploader<E> uploader = uploaders.get(renderChunk);
    // if a key for this object already exists, notify the shutdown hook and remove the old entry
    if (uploader != null && shutdownTask != null) {
      shutdownTask.accept(uploader);
      uploaders.remove(renderChunk);
    }
  }

  /**
   * Unregister all added RenderChunks
   */
  public void unregisterAll() {
    forEach((k, v) -> unregister(k));
  }

  public Optional<RenderUploader<E>> get(ChunkRenderDispatcher.RenderChunk renderChunk) {
    return Optional.ofNullable(uploaders.get(renderChunk));
  }

  /**
   * Current size of the RenderChunk map
   */
  public int size() {
    return uploaders.size();
  }

  public void computeIfPresent(
      ChunkRenderDispatcher.RenderChunk renderChunk, final Consumer<RenderUploader<E>> task) {
    RenderUploader<E> uploader = uploaders.get(renderChunk);
    if (uploader != null) {
      task.accept(uploader);
    }
  }

  public void forEach(BiConsumer<ChunkRenderDispatcher.RenderChunk, RenderUploader<E>> action) {
    uploaders.forEach(action);
  }

  /**
   * Set the shutdown hook for when a RenderUploader is no longer needed
   *
   * @param shutdownTask
   *     task
   */
  public void onShutdown(Consumer<RenderUploader<E>> shutdownTask) {
    this.shutdownTask = shutdownTask;
  }

  public TessellatorCache<E> cache() {
    return cache;
  }

  public interface UploaderSupplier<T extends Tesselator> {

    RenderUploader<T> get(Uploaders<T> parent);
  }
}
