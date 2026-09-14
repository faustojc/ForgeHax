package com.matt.forgehax.util;

import com.google.common.collect.Lists;
import com.matt.forgehax.mixin.accessor.BlockItemInvoker;
import com.matt.forgehax.util.entity.LocalPlayerInventory.InvItem;
import com.matt.forgehax.util.entity.LocalPlayerUtils;
import com.matt.forgehax.util.math.VectorUtils;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;
import java.util.stream.Stream;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.util.entity.LocalPlayerUtils.isInReach;

public class BlockHelper {

  public static UniqueBlock newUniqueBlock(BlockState state, BlockPos pos) {
    return new UniqueBlock(state, pos);
  }

  public static UniqueBlock newUniqueBlock(BlockPos pos) {
    return newUniqueBlock(getWorld().getBlockState(pos), pos);
  }

  public static BlockTraceInfo newBlockTrace(BlockPos pos, Direction side) {
    return new BlockTraceInfo(pos, side);
  }

  public static List<BlockPos> getBlocksInRadius(Vec3 pos, double radius) {
    List<BlockPos> list = Lists.newArrayList();
    for (double x = pos.x - radius; x <= pos.x + radius; ++x) {
      for (double y = pos.y - radius; y <= pos.y + radius; ++y) {
        for (double z = pos.z - radius; z <= pos.z + radius; ++z) {
          list.add(new BlockPos((int) x, (int) y, (int) z));
        }
      }
    }
    return list;
  }

  public static float getBlockHardness(BlockPos pos) {
    return getWorld().getBlockState(pos).getDestroySpeed(getWorld(), pos);
  }

  public static boolean isBlockReplaceable(BlockPos pos) {
    return getWorld().getBlockState(pos).canBeReplaced();
  }

  public static boolean isTraceClear(Vec3 start, Vec3 end, Direction targetSide) {
    BlockHitResult tr = getWorld().clip(new ClipContext(
        start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, getLocalPlayer()));
    return tr.getType() == HitResult.Type.MISS
        || (BlockPos.containing(end).equals(tr.getBlockPos())
        && targetSide.getOpposite().equals(tr.getDirection()));
  }

  public static Vec3 getOBBCenter(BlockPos pos) {
    BlockState state = getWorld().getBlockState(pos);
    VoxelShape shape = state.getShape(getWorld(), pos);
    if (shape.isEmpty()) {
      return Vec3.ZERO;
    }
    AABB bb = shape.bounds();
    return new Vec3(
        bb.minX + ((bb.maxX - bb.minX) / 2.D),
        bb.minY + ((bb.maxY - bb.minY) / 2.D),
        bb.minZ + ((bb.maxZ - bb.minZ) / 2.D)
    );
  }

  public static boolean isBlockPlaceable(BlockPos pos) {
    // equivalent of the old Block#canCollideCheck(state, false), which reduced to
    // "does this state's material block movement"
    return getWorld().getBlockState(pos).blocksMotion();
  }

  private static BlockTraceInfo getPlaceableBlockSideTrace(
      Vec3 eyes, Vec3 normal, Stream<Direction> stream, BlockPos pos) {
    return stream
        .map(side -> newBlockTrace(pos.relative(side), side))
        .filter(info -> isBlockPlaceable(info.getPos()))
        .filter(info -> isInReach(eyes, info.getHitVec()))
        .filter(info -> BlockHelper.isTraceClear(eyes, info.getHitVec(), info.getSide()))
        .min(
            Comparator.<BlockTraceInfo>comparingInt(info -> info.isSneakRequired() ? 1 : 0)
                      .thenComparing(
                          info -> VectorUtils.getCrosshairDistance(eyes, normal, info.getCenteredPos())))
        .orElse(null);
  }

  public static BlockTraceInfo getPlaceableBlockSideTrace(
      Vec3 eyes, Vec3 normal, EnumSet<Direction> sides, BlockPos pos) {
    return getPlaceableBlockSideTrace(eyes, normal, sides.stream(), pos);
  }

  public static BlockTraceInfo getPlaceableBlockSideTrace(Vec3 eyes, Vec3 normal, BlockPos pos) {
    return getPlaceableBlockSideTrace(eyes, normal, Stream.of(Direction.values()), pos);
  }

  public static BlockTraceInfo getBlockSideTrace(Vec3 eyes, BlockPos pos, Direction side) {
    return Optional.of(newBlockTrace(pos, side))
                   .filter(tr -> BlockHelper.isTraceClear(eyes, tr.getHitVec(), tr.getSide()))
                   .filter(tr -> LocalPlayerUtils.isInReach(eyes, tr.getHitVec()))
                   .orElse(null);
  }

  public static BlockTraceInfo getVisibleBlockSideTrace(Vec3 eyes, Vec3 normal, BlockPos pos) {
    return Arrays.stream(Direction.values())
                 .map(side -> BlockHelper.getBlockSideTrace(eyes, pos, side.getOpposite()))
                 .filter(Objects::nonNull)
                 .min(
                     Comparator.comparingDouble(
                         i -> VectorUtils.getCrosshairDistance(eyes, normal, i.getCenteredPos())))
                 .orElse(null);
  }

  public static class BlockTraceInfo {

    private final BlockPos pos;
    private final Direction side;
    private final Vec3 center;
    private final Vec3 hitVec;

    private BlockTraceInfo(BlockPos pos, Direction side) {
      this.pos = pos;
      this.side = side;
      Vec3 obb = BlockHelper.getOBBCenter(pos);
      this.center = new Vec3(pos.getX(), pos.getY(), pos.getZ()).add(obb);
      Direction opposite = getOppositeSide();
      this.hitVec =
          this.center.add(
              VectorUtils.multiplyBy(
                  new Vec3(opposite.getStepX(), opposite.getStepY(), opposite.getStepZ()), obb));
    }

    public BlockPos getPos() {
      return pos;
    }

    public Direction getSide() {
      return side;
    }

    public Direction getOppositeSide() {
      return side.getOpposite();
    }

    public Vec3 getHitVec() {
      return this.hitVec;
    }

    public Vec3 getCenteredPos() {
      return center;
    }

    public BlockState getBlockState() {
      return getWorld().getBlockState(getPos());
    }

    public boolean isPlaceable(InvItem item) {
      if (!(item.getItem() instanceof BlockItem)) {
        return true;
      }

      BlockPlaceContext context =
          new BlockPlaceContext(
              getLocalPlayer(),
              InteractionHand.MAIN_HAND,
              item.getItemStack(),
              new BlockHitResult(getHitVec(), getOppositeSide(), getPos(), false));
      return ((BlockItemInvoker) item.getItem()).invokeGetPlacementState(context) != null;
    }

    public boolean isSneakRequired() {
      return BlockActivationChecker.isOverwritten(getBlockState().getBlock());
    }
  }

  public static class UniqueBlock {

    private final BlockState state;
    private final BlockPos pos;

    private UniqueBlock(BlockState state, BlockPos pos) {
      this.state = state;
      this.pos = pos;
    }

    public Block getBlock() {
      return state.getBlock();
    }

    public BlockState getState() {
      return state;
    }

    /** Global block-state id (see {@link com.matt.forgehax.util.blocks.BlockOptionHelper}). */
    public int getMetadata() {
      return Block.getId(state);
    }

    public BlockPos getPos() {
      return pos;
    }

    public Vec3 getCenteredPos() {
      return new Vec3(getPos().getX(), getPos().getY(), getPos().getZ()).add(getOBBCenter(getPos()));
    }

    public ItemStack asItemStack() {
      return new ItemStack(getBlock());
    }

    public boolean isInvalid() {
      return Blocks.AIR.equals(getBlock());
    }

    public boolean isEqual(BlockPos pos) {
      return Objects.equals(getState(), getWorld().getBlockState(pos));
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj
          || (obj instanceof UniqueBlock && getState().equals(((UniqueBlock) obj).getState()));
    }

    @Override
    public String toString() {
      return BuiltInRegistries.BLOCK.getKey(getBlock()).toString() + "{" + getMetadata() + "}";
    }
  }

  public static class BlockActivationChecker {

    private static final Object2BooleanArrayMap<Class<?>> CACHE = new Object2BooleanArrayMap<>();

    public static boolean isOverwritten(final Block instance) {
      Objects.requireNonNull(instance);
      return CACHE.computeIfAbsent(
          instance.getClass(),
          (Class<?> clazz) -> {
            try {
              Class<?> declaring = clazz.getMethod(
                  "use",
                  BlockState.class,
                  net.minecraft.world.level.Level.class,
                  BlockPos.class,
                  net.minecraft.world.entity.player.Player.class,
                  net.minecraft.world.InteractionHand.class,
                  BlockHitResult.class
              ).getDeclaringClass();
              return declaring != BlockBehaviour.class;
            } catch (NoSuchMethodException e) {
              return false;
            }
          }
      );
    }
  }
}
