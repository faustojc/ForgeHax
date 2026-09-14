package com.matt.forgehax.mixin.accessor;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(BlockItem.class)
public interface BlockItemInvoker {

  @Nullable
  @Invoker("getPlacementState")
  BlockState invokeGetPlacementState(BlockPlaceContext context);
}
