package com.matt.forgehax.util;

import com.matt.forgehax.Globals;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.math.Angle;
import com.matt.forgehax.util.math.AngleHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.matt.forgehax.Helper.getLocalPlayer;

public class Utils implements Globals {

  public static <E extends Enum<?>> String[] toArray(E[] o) {
    String[] output = new String[o.length];
    for (int i = 0; i < output.length; i++) {
      output[i] = o[i].name();
    }
    return output;
  }

  public static UUID stringToUUID(String uuid) {
    if (uuid.contains("-")) {
      // if it contains the hyphen we don't have to manually put them in
      return UUID.fromString(uuid);
    } else {
      // otherwise we have to put
      Pattern pattern = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
      Matcher matcher = pattern.matcher(uuid);
      return UUID.fromString(matcher.replaceAll("$1-$2-$3-$4-$5"));
    }
  }

  public static double normalizeAngle(double angle) {
    while (angle <= -180) {
      angle += 360;
    }
    while (angle > 180) {
      angle -= 360;
    }
    return angle;
  }

  public static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  public static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }

  public static Angle getLookAtAngles(Vec3 start, Vec3 end) {
    return AngleHelper.getAngleFacingInDegrees(end.subtract(start)).normalize();
  }

  public static Angle getLookAtAngles(Vec3 end) {
    return getLookAtAngles(EntityUtils.getEyePos(getLocalPlayer()), end);
  }

  public static Angle getLookAtAngles(Entity entity) {
    return getLookAtAngles(EntityUtils.getOBBCenter(entity));
  }

  public static double scale(
      double x, double from_min, double from_max, double to_min, double to_max) {
    return to_min + (to_max - to_min) * ((x - from_min) / (from_max - from_min));
  }

  public static <T> boolean isInRange(Collection<T> list, int index) {
    return list != null && index >= 0 && index < list.size();
  }

  public static <T> T defaultTo(T value, T defaultTo) {
    return value == null ? defaultTo : value;
  }

  public static List<ItemStack> getShulkerContents(ItemStack stack) { // TODO: move somewhere else
    NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
    CompoundTag compound = stack.getTag();
    if (compound != null && compound.contains("BlockEntityTag", 10)) {
      CompoundTag tags = compound.getCompound("BlockEntityTag");
      if (tags.contains("Items", 9)) {
        // load in the items
        ContainerHelper.loadAllItems(tags, contents);
      }
    }
    return contents;
  }
}
