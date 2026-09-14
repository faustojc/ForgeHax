package com.matt.forgehax.util.markers;

import com.matt.forgehax.Globals;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created on 1/18/2018 by fr1kin
 */
public class RenderUploader<E extends Tesselator> implements Globals {

  private final Uploaders<E> parent;
  private final ReentrantLock _lock = new ReentrantLock();

  /**
   * The vertex buffer instance (for uploading the tessellator data)
   */
  private final VertexBuffer vertexBuffer;

  /**
   * The tessellator instance
   */
  private volatile E tessellator;

  /**
   * The thread the current VBO is being processed on
   */
  private volatile Thread currentThread;

  private boolean complete = false;
  private boolean uploaded = false;
  private int renderCount = 0;
  private BlockPos region = null;

  /**
   * Buffer produced by {@link #finishDrawing()}, staged until {@link #upload()} sends it to the GPU.
   */
  private BufferBuilder.RenderedBuffer pendingBuffer;

  public RenderUploader(Uploaders<E> parent, VertexFormat format) {
    this.parent = parent;
    // TODO(1.20.1): VertexBuffer no longer takes a VertexFormat at construction time - the format is
    // inferred from the BufferBuilder's DrawState when upload() is called. Parameter kept for API
    // compatibility with callers built around the 1.12.2 signature.
    vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
  }

  public RenderUploader(Uploaders<E> parent) {
    this(parent, DefaultVertexFormat.POSITION_COLOR);
  }

  public boolean isComplete() {
    return complete;
  }

  public void setComplete(boolean complete) {
    this.complete = complete;
  }

  /**
   * Gets the tessellator
   *
   * @return volatile tessellator
   */
  public E getTessellator() {
    return tessellator;
  }

  public void setTessellator(E tessellator) throws UploaderException {
    if (tessellator != null && this.tessellator != null) {
      throw new UploaderException("Tried to set tessellator without removing the previous one");
    } else {
      this.tessellator = tessellator;
    }
  }

  public void takeTessellator() throws UploaderException, InterruptedException {
    setTessellator(parent.cache().take());
  }

  public void freeTessellator()
      throws UploaderException, TessellatorCache.TessellatorCacheFreeException {
    if (tessellator != null) {
      // stop tessellator from drawing
      if (isTessellatorDrawing()) {
        finishDrawing();
      }
      // discard any geometry that was never uploaded
      if (pendingBuffer != null) {
        pendingBuffer.release();
        pendingBuffer = null;
      }
      // free tessellator to parent
      parent.cache().free(tessellator);
      // set tessellator to null
      setTessellator(null);
    }
  }

  public BufferBuilder getBufferBuilder() {
    return getTessellator().getBuilder();
  }

  /**
   * Will set the current thread to the current thread running the method.
   */
  public void setCurrentThread() {
    currentThread = Thread.currentThread();
  }

  public void nullifyCurrentThread() {
    currentThread = null;
  }

  /**
   * Verify that the current thread is the one doing the rendering
   *
   * @throws ThreadMismatchException
   *     if running in the incorrect thread
   */
  public void validateCurrentThread() throws ThreadMismatchException {
    if (currentThread != Thread.currentThread()) {
      throw new ThreadMismatchException("Tried executing in incorrect thread (this is normal)");
    }
  }

  /**
   * Same thing but returns a boolean instead of throwing error
   */
  public boolean isCorrectCurrentThread() {
    return currentThread == Thread.currentThread();
  }

  /**
   * Get the vertex buffer used to upload
   */
  public VertexBuffer getVertexBuffer() {
    return vertexBuffer;
  }

  /**
   * Upload the vertex buffer to the GPU
   *
   * @throws UploaderException
   *     if the upload failed
   */
  public boolean upload() throws UploaderException {
    if (getTessellator() == null) {
      return false; // no tessellator
    }
    if (!RenderSystem.isOnRenderThread()) {
      throw new UploaderException("Not calling from main Minecraft thread");
    }

    boolean update = false;

    lock().lock();
    try {
      if (isTessellatorDrawing()) {
        finishDrawing(); // force to stop, stages pendingBuffer
        update = true;
      }

      if (pendingBuffer != null) {
        vertexBuffer.bind();
        vertexBuffer.upload(pendingBuffer); // upload() releases the RenderedBuffer internally
        VertexBuffer.unbind();
        pendingBuffer = null;
      }
    } finally {
      setComplete(true);
      uploaded = true;
      lock().unlock();
    }
    return update;
  }

  /**
   * Unload the vertex buffer from the GPU
   *
   * @throws UploaderException
   *     if the unload failed
   */
  public void unload() throws UploaderException {
    if (!isUploaded()) {
      return;
    }
    if (!RenderSystem.isOnRenderThread()) {
      throw new UploaderException("Not calling from main Minecraft thread");
    }

    try {
      vertexBuffer.close();
    } finally {
      uploaded = false;
      setComplete(false);
      resetRegion();
    }
  }

  /**
   * Check if the tessellator instance is current drawing
   */
  public boolean isTessellatorDrawing() {
    return getTessellator() != null && getBufferBuilder().building();
  }

  public void finishDrawing() {
    if (!isTessellatorDrawing()) {
      return;
    }

    pendingBuffer = getBufferBuilder().end();
    renderCount = pendingBuffer.drawState().vertexCount() / 24;
  }

  /**
   * Check if the vertex buffer has been uploaded
   */
  public boolean isUploaded() {
    return uploaded;
  }

  /**
   * Number of objects currently added to the tessellator
   *
   * @return number
   */
  public int getRenderCount() {
    return renderCount;
  }

  public void resetRenderCount() {
    renderCount = 0;
  }

  public void resetRegion() {
    region = null;
  }

  public BlockPos getRegion() {
    return region;
  }

  public void setRegion(ChunkRenderDispatcher.RenderChunk chunk) {
    region = new BlockPos(chunk.getOrigin()); // copy because RenderChunk.origin is mutable
  }

  public boolean isCorrectRegion(ChunkRenderDispatcher.RenderChunk chunk) {
    return region != null && region.equals(chunk.getOrigin());
  }

  public ReentrantLock lock() {
    return _lock;
  }

  public static class UploaderException extends Exception {

    public UploaderException(String msg) {
      super(msg);
    }
  }

  public static class ThreadMismatchException extends UploaderException {

    public ThreadMismatchException(String msg) {
      super(msg);
    }
  }

  public static class VertexBufferException extends UploaderException {

    public VertexBufferException(String msg) {
      super(msg);
    }
  }
}
