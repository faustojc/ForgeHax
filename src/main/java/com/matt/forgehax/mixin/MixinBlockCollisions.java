package com.matt.forgehax.mixin;

import com.matt.forgehax.asm.ForgeHaxHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stand-in for the 1.12.2 {@code Block#addCollisionBoxToList} patch. That method is gone; the
 * per-block collision lookup now happens here, and this is the only place with both the entity
 * and the swept entity box in scope.
 *
 * <p>Listener-supplied boxes are in world space, and the caller shifts the returned shape by the
 * block position afterwards, so they are moved back into block-local space here.
 */
@Mixin(BlockCollisions.class)
public abstract class MixinBlockCollisions {

  @Shadow @Final private AABB box;

  @Shadow @Final private CollisionContext context;

  @Shadow @Final private CollisionGetter collisionGetter;

  @Redirect(
      method = "computeNext",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/block/state/BlockState;getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
  private VoxelShape forgehax$onAddCollisionBoxToList(
      BlockState state, BlockGetter getter, BlockPos pos, CollisionContext ctx) {
    VoxelShape vanilla = state.getCollisionShape(getter, pos, ctx);

    Entity entity = forgehax$entity();
    if (!ForgeHaxHooks.shouldFireCollisionBoxes(entity)) {
      return vanilla;
    }

    BlockPos immutable = pos.immutable();
    List<AABB> added = new ArrayList<>();
    boolean canceled =
        ForgeHaxHooks.onAddCollisionBoxToList(
            state.getBlock(),
            state,
            collisionGetter instanceof Level ? (Level) collisionGetter : null,
            immutable,
            box,
            added,
            entity,
            false);

    if (added.isEmpty()) {
      return canceled ? Shapes.empty() : vanilla;
    }

    VoxelShape shape = canceled ? Shapes.empty() : vanilla;
    for (AABB aabb : added) {
      shape =
          Shapes.or(
              shape,
              Shapes.create(
                  aabb.move(-immutable.getX(), -immutable.getY(), -immutable.getZ())));
    }
    return shape;
  }

  @Nullable
  private Entity forgehax$entity() {
    return context instanceof EntityCollisionContext
        ? ((EntityCollisionContext) context).getEntity()
        : null;
  }
}
