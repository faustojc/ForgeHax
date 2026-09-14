package com.matt.forgehax.util.blocks.properties;

import com.google.common.collect.Sets;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created on 5/23/2017 by fr1kin
 *
 * <p>TODO(1.20.1): dimensions were switched from integer ids to registry keys
 * ({@code ResourceKey<Level>}, e.g. {@code Level.OVERWORLD}/{@code Level.NETHER}/
 * {@code Level.END}) - there's no int-id lookup anymore.
 */
public class DimensionProperty implements IBlockProperty {

  private static final String HEADING = "dimensions";

  private final Collection<ResourceKey<Level>> dimensions = Sets.newHashSet();

  public boolean add(ResourceKey<Level> dimension) {
    return dimension != null && dimensions.add(dimension);
  }

  public boolean remove(ResourceKey<Level> dimension) {
    return dimension != null && dimensions.remove(dimension);
  }

  public boolean contains(ResourceKey<Level> dimension) {
    return dimensions.isEmpty() || dimensions.contains(dimension); // true if none other
  }

  @Override
  public void serialize(JsonWriter writer) throws IOException {
    writer.beginArray();
    for (ResourceKey<Level> dimension : dimensions) {
      writer.value(dimension.location().toString());
    }
    writer.endArray();
  }

  @Override
  public void deserialize(JsonReader reader) throws IOException {
    reader.beginArray();
    while (reader.hasNext() && reader.peek().equals(JsonToken.STRING)) {
      String dim = reader.nextString();
      ResourceLocation location = ResourceLocation.tryParse(dim);
      if (location != null) {
        add(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location));
      }
    }
    reader.endArray();
  }

  @Override
  public boolean isNecessary() {
    return !dimensions.isEmpty();
  }

  @Override
  public String helpText() {
    final StringBuilder builder = new StringBuilder("{");
    Iterator<ResourceKey<Level>> it = dimensions.iterator();
    while (it.hasNext()) {
      String name = it.next().location().toString();
      builder.append(name);
      if (it.hasNext()) {
        builder.append(", ");
      }
    }
    builder.append("}");
    return builder.toString();
  }

  @Override
  public IBlockProperty newImmutableInstance() {
    return new ImmutableDimension();
  }

  @Override
  public String toString() {
    return HEADING;
  }

  private static class ImmutableDimension extends DimensionProperty {

    @Override
    public boolean add(ResourceKey<Level> dimension) {
      return false;
    }

    @Override
    public boolean remove(ResourceKey<Level> dimension) {
      return false;
    }

    @Override
    public boolean contains(ResourceKey<Level> dimension) {
      return true; // Allow ALL dimensions by default
    }
  }
}
