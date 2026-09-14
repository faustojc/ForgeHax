package com.matt.forgehax.util.blocks;

import com.matt.forgehax.Helper;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.matt.forgehax.Helper.getFileManager;

/** Main-thread block-position index with serialized, single-threaded cache I/O. */
public final class PersistentBlockCache {
  private static final ExecutorService IO =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "ForgeHax-BlockCache");
            thread.setDaemon(true);
            return thread;
          });
  private final Map<Long, Map<Long, Integer>> chunks = new HashMap<>();
  private Path file;
  private int generation;
  private boolean dirty;

  public static Path pathFor(String module, int dimension) {
    ServerData server = Helper.getMinecraft().getCurrentServer();
    String identity =
        server != null
            ? server.ip
            : Helper.getWorld() != null ? "singleplayer" : "unknown";
    String worldId =
        UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    return getFileManager()
        .getMkBaseResolve("cache", "block-esp", module + "-" + worldId + "-" + dimension + ".csv");
  }

  private static long chunkKey(int x, int z) {
    return (x & 0xffffffffL) | ((z & 0xffffffffL) << 32);
  }

  private static Map<Long, Integer> read(Path path) {
    Map<Long, Integer> loaded = new HashMap<>();
    if (!Files.isRegularFile(path)) {
      return loaded;
    }
    try {
      for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
        String[] fields = line.split(",", 2);
        if (fields.length == 2) {
          loaded.put(Long.parseLong(fields[0]), Integer.parseInt(fields[1]));
        }
      }
    } catch (IOException | NumberFormatException exception) {
      exception.printStackTrace();
    }
    return loaded;
  }

  private static void write(Path destination, List<String> lines) {
    Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
    try {
      Files.createDirectories(destination.getParent());
      Files.write(temporary, lines, StandardCharsets.UTF_8);
      try {
        Files.move(
            temporary,
            destination,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        );
      } catch (IOException ignored) {
        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  public void open(Path path) {
    if (path.equals(file)) {
      return;
    }
    flush();
    file = path;
    chunks.clear();
    dirty = false;
    final int expectedGeneration = ++generation;
    IO.execute(
        () -> {
          final Map<Long, Integer> loaded = read(path);
          Helper.getMinecraft()
                .execute(
                    () -> {
                      if (generation == expectedGeneration && path.equals(file)) {
                        for (Map.Entry<Long, Integer> entry : loaded.entrySet()) {
                          putIfAbsent(BlockPos.of(entry.getKey()), entry.getValue());
                        }
                      }
                    });
        });
  }

  public boolean put(BlockPos pos, int color) {
    Map<Long, Integer> entries =
        chunks.computeIfAbsent(chunkKey(pos.getX() >> 4, pos.getZ() >> 4), ignored -> new HashMap<>());
    Integer previous = entries.put(pos.asLong(), color);
    boolean changed = previous == null || previous.intValue() != color;
    dirty |= changed;
    return changed;
  }

  public boolean remove(BlockPos pos) {
    long key = chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
    Map<Long, Integer> entries = chunks.get(key);
    if (entries == null || entries.remove(pos.asLong()) == null) {
      return false;
    }
    if (entries.isEmpty()) {
      chunks.remove(key);
    }
    dirty = true;
    return true;
  }

  public void forEachNearby(BlockPos center, int distance, Visitor visitor) {
    int minChunkX = (center.getX() - distance) >> 4;
    int maxChunkX = (center.getX() + distance) >> 4;
    int minChunkZ = (center.getZ() - distance) >> 4;
    int maxChunkZ = (center.getZ() + distance) >> 4;
    double distanceSquared = (double) distance * distance;
    for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
      for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
        Map<Long, Integer> entries = chunks.get(chunkKey(chunkX, chunkZ));
        if (entries == null) {
          continue;
        }
        for (Map.Entry<Long, Integer> entry : entries.entrySet()) {
          BlockPos pos = BlockPos.of(entry.getKey());
          if (pos.distSqr(center) <= distanceSquared) {
            visitor.visit(pos, entry.getValue());
          }
        }
      }
    }
  }

  public void flush() {
    final Path destination = file;
    if (!dirty || destination == null) {
      return;
    }
    final List<String> lines = new ArrayList<>();
    for (Map<Long, Integer> entries : chunks.values()) {
      for (Map.Entry<Long, Integer> entry : entries.entrySet()) {
        lines.add(entry.getKey() + "," + entry.getValue());
      }
    }
    dirty = false;
    IO.execute(() -> write(destination, lines));
  }

  public void close() {
    flush();
    file = null;
    chunks.clear();
    dirty = false;
    generation++;
  }

  private void putIfAbsent(BlockPos pos, int color) {
    chunks
        .computeIfAbsent(chunkKey(pos.getX() >> 4, pos.getZ() >> 4), ignored -> new HashMap<>())
        .putIfAbsent(pos.asLong(), color);
  }

  public interface Visitor {
    void visit(BlockPos pos, int color);
  }
}
