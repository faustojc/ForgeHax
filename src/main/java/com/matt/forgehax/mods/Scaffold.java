package com.matt.forgehax.mods;

import com.matt.forgehax.mixin.accessor.MinecraftAccessor;
import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.mods.managers.PositionRotationManager.RotationState.Local;
import com.matt.forgehax.mods.services.HotbarSelectionService.ResetFunction;
import com.matt.forgehax.util.BlockHelper;
import com.matt.forgehax.util.BlockHelper.BlockTraceInfo;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.Utils;
import com.matt.forgehax.util.common.PriorityEnum;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.LocalPlayerInventory;
import com.matt.forgehax.util.entity.LocalPlayerInventory.InvItem;
import com.matt.forgehax.util.entity.LocalPlayerUtils;
import com.matt.forgehax.util.math.Angle;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import static com.matt.forgehax.Helper.*;

@RegisterMod
public class Scaffold extends ToggleMod implements PositionRotationManager.MovementUpdateListener {

  private static final EnumSet<Direction> NEIGHBORS =
      EnumSet.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

  private int tickCount = 0;
  private boolean placing = false;
  private Angle previousAngles = Angle.ZERO;

  public Scaffold() {
    super(Category.PLAYER, "Scaffold", false, "Place blocks under yourself");
  }

  @Override
  protected void onEnabled() {
    PositionRotationManager.getManager().register(this, PriorityEnum.HIGHEST);
  }

  @Override
  protected void onDisabled() {
    PositionRotationManager.getManager().unregister(this);
  }

  @Override
  public void onLocalPlayerMovementUpdate(Local state) {
    if (placing) {
      ++tickCount;
    }

    if (LocalPlayerUtils.getVelocity().normalize().length() > 1.D && placing) {
      state.setServerAngles(previousAngles);
    } else {
      placing = false;
      tickCount = 0;
    }

    BlockPos below = getLocalPlayer().blockPosition().below();

    if (!getWorld().getBlockState(below).canBeReplaced()) {
      return;
    }

    InvItem items =
        LocalPlayerInventory.getHotbarInventory()
                            .stream()
                            .filter(InvItem::nonNull)
                            .filter(item -> item.getItem() instanceof BlockItem)
                            .filter(item -> Block.isShapeFullBlock(
                                Block.byItem(item.getItem())
                                     .defaultBlockState()
                                     .getShape(getWorld(), BlockPos.ZERO)))
                            .max(Comparator.comparingInt(LocalPlayerInventory::getHotbarDistance))
                            .orElse(InvItem.EMPTY);

    if (items.isNull()) {
      return;
    }

    final Vec3 eyes = EntityUtils.getEyePos(getLocalPlayer());
    final Vec3 dir = LocalPlayerUtils.getViewAngles().getDirectionVector();

    BlockTraceInfo trace =
        Optional.ofNullable(BlockHelper.getPlaceableBlockSideTrace(eyes, dir, below))
                .filter(tr -> tr.isPlaceable(items))
                .orElseGet(
                    () ->
                        NEIGHBORS
                            .stream()
                            .map(below::relative)
                            .filter(BlockHelper::isBlockReplaceable)
                            .map(bp -> BlockHelper.getPlaceableBlockSideTrace(eyes, dir, bp))
                            .filter(Objects::nonNull)
                            .filter(tr -> tr.isPlaceable(items))
                            .max(Comparator.comparing(BlockTraceInfo::isSneakRequired))
                            .orElse(null));

    if (trace == null) {
      return;
    }

    Vec3 hit = trace.getHitVec();
    state.setServerAngles(previousAngles = Utils.getLookAtAngles(hit));

    final BlockTraceInfo tr = trace;
    state.invokeLater(
        rs -> {
          ResetFunction func = LocalPlayerInventory.setSelected(items);

          boolean sneak = tr.isSneakRequired() && !LocalPlayerUtils.isSneaking();
          if (sneak) {
            // send start sneaking packet
            PacketHelper.ignoreAndSend(
                new ServerboundPlayerCommandPacket(getLocalPlayer(), Action.PRESS_SHIFT_KEY));

            LocalPlayerUtils.setSneaking(true);
            LocalPlayerUtils.setSneakingSuppression(true);
          }

          getPlayerController()
              .useItemOn(
                  getLocalPlayer(),
                  InteractionHand.MAIN_HAND,
                  new BlockHitResult(hit, tr.getOppositeSide(), tr.getPos(), false)
              );

          getNetworkManager().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

          if (sneak) {
            LocalPlayerUtils.setSneaking(false);
            LocalPlayerUtils.setSneakingSuppression(false);

            getNetworkManager()
                .send(new ServerboundPlayerCommandPacket(getLocalPlayer(), Action.RELEASE_SHIFT_KEY));
          }

          func.revert();

          // the right-click delay moved off PlayerControllerMP onto Minecraft in modern MC
          ((MinecraftAccessor) MC).setRightClickDelay(4);
          placing = true;
          tickCount = 0;
        });
  }
}
