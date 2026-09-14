package com.matt.forgehax.asm.events.listeners;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Created on 5/8/2017 by fr1kin
 */
public interface BlockModelRenderListener extends ListenerHook {

  void onBlockRenderInLoop(RenderChunk renderChunk, Block block, BlockState state, BlockPos pos);
}
