package com.matt.forgehax.mods;

import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.matt.forgehax.Helper.getFileManager;


@RegisterMod
public class BreadCrumbs extends ToggleMod {

  private static final Path BASE_PATH = getFileManager().getBaseResolve("breadcrumbs");
  public final Setting<Integer> smoothness =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("smoothness")
          .description("rendering smoothness")
          .defaultTo(1)
          .min(1)
          .max(20)
          .build();

  /*public final Setting<Boolean> simplify =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("simplify")
          .description("Simplify the path")
          .defaultTo(false)
          .build();*/
  public final Setting<Integer> maxpoints =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("maxpoints")
          .description("maximum number of points to save")
          .defaultTo(20000)
          .min(1)
          .max(100000)
          .build();
  public final Setting<Boolean> drawIntermediate =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("intermediate") // TODO: give this a better name
          .description("Draw points between anchors")
          .defaultTo(true)
          .build();
  private final List<Trail> trails = new ArrayList<>();
  private boolean recording = true; // TODO: use this
  private Anchor rootAnchor;
  private Anchor newestAnchor;
  private Set<Anchor> visibleLastTick = Collections.emptySet();
  private int dimension;
  private List<Anchor> currentPath = Collections.emptyList();
  private List<List<Vec3d>> pointsToDraw = Collections.emptyList();
  private boolean graphDirty = true;
  private boolean renderSnapshotDirty = true;
  private int cachedSmoothness = -1;
  private boolean cachedDrawIntermediate;
  private int cachedMaxPoints = -1;
  private int recordedPointCount;
  private int graphAnchorCount;
  private boolean topologyPruned;

  public BreadCrumbs() {
    super(Category.RENDER, "BreadCrumbs", false, "epic trail meme");
  }

  private static boolean isVisible(Anchor anchor) {
    if (!MC.world.isAreaLoaded(new BlockPos(anchor.pos), 1, false)) {
      return false;
    } else {
      Vec3d from = new Vec3d(MC.player.posX, MC.player.posY, MC.player.posZ);
      RayTraceResult result = MC.world.rayTraceBlocks(from, anchor.pos, false, true, true);
      return result == null || result.typeOfHit == RayTraceResult.Type.MISS;
    }
  }

  private static AnchorVisibility getAnchorVisibility(Anchor root) {
    final Set<Anchor> all = new HashSet<>();
    final Set<Anchor> visible = new HashSet<>();
    getAnchorVisibility(root, all, visible);
    return new AnchorVisibility(all, visible);
  }

  /*private static class AnchorGraph {
    final Anchor root;
    final Set<Anchor> vertices = new HashSet<>();
  }*/

  private static void getAnchorVisibility(
      Anchor anchor, Set<Anchor> all, Set<Anchor> visible) {
    if (!all.add(anchor)) return;
    if (isVisible(anchor)) visible.add(anchor);
    for (Anchor connected : anchor.connected) {
      getAnchorVisibility(connected, all, visible);
    }
  }

  private static List<Anchor> getPath(Anchor source, Anchor target, Map<Anchor, Anchor> prev) {
    final List<Anchor> out = new ArrayList<>();
    Anchor it = target;
    if (prev.containsKey(it) || it == source) {
      while (it != null) {
        out.add(it); // supposed to add to the beginning of the list but returning a reverse view will have the same effect
        it = prev.get(it);
      }
    }
    Collections.reverse(out);
    return out;
  }

  /*private static Anchor minAnchor(Anchor root, Comparator<Anchor> comparator) {
    return minAnchor(root, root, comparator);
  }

  private static Anchor minAnchor(Anchor root, Anchor min, Comparator<Anchor> comparator) {
    if (comparator.compare(root, min) < 0) { // TODO: make sure < 0 is correct
      min = root;
    }
    for (Anchor child : root.connected) {
      min = minAnchor(child, min, comparator);
    }
    return min;
  }*/

  private static double length(Anchor a, Anchor b) {
    return a.pos.distanceTo(b.pos);
  }

  private static List<Anchor> pathFind(Anchor root, Anchor target) {
    final Map<Anchor, Double> distances = new HashMap<>();
    distances.put(root, 0.D);
    final Map<Anchor, Anchor> prev = new HashMap<>();
    final PriorityQueue<PathNode> queue =
        new PriorityQueue<>(Comparator.comparingDouble(node -> node.distance));
    queue.add(new PathNode(root, 0.D));

    while (!queue.isEmpty()) {
      final PathNode node = queue.poll();
      final Anchor u = node.anchor;
      if (node.distance > distances.getOrDefault(u, Double.POSITIVE_INFINITY)) continue;
      if (u == target) break;

      for (Anchor v : u.connected) {
        final double alt = distances.get(u) + length(u, v);
        if (alt < distances.getOrDefault(v, Double.POSITIVE_INFINITY)) {
          distances.put(v, alt);
          prev.put(v, u);
          queue.add(new PathNode(v, alt));
        }
      }
    }

    return getPath(root, target, prev);
  }

  @SubscribeEvent
  public void onPlayerUpdate(LocalPlayerUpdateEvent event) {
    if (Helper.getModManager().get(FreecamMod.class).map(ToggleMod::isEnabled).orElse(false)) return;

    if (this.recording) {
      final Vec3d playerPos = new Vec3d(MC.player.posX, MC.player.posY, MC.player.posZ);

      // first tick of a trail
      if (this.rootAnchor == null) {
        this.rootAnchor = new Anchor(playerPos);
        this.newestAnchor = rootAnchor;
        this.dimension = MC.player.dimension;
        this.graphAnchorCount = 1;
        this.recordedPointCount++;
        this.visibleLastTick = Collections.emptySet();
        this.graphDirty = true;
        this.renderSnapshotDirty = true;
      } else {
        if (this.topologyPruned) {
          this.visibleLastTick = new HashSet<>(this.visibleLastTick);
          this.visibleLastTick.retainAll(this.currentPath);
          this.topologyPruned = false;
        }
        final AnchorVisibility anchorVisibility = getAnchorVisibility(this.rootAnchor);
        final Set<Anchor> visibleAnchors = anchorVisibility.visible;
        final Set<Anchor> noLongerVisible = new HashSet<>(this.visibleLastTick);
        noLongerVisible.removeAll(visibleAnchors);

        final Set<Anchor> allOldAnchors = anchorVisibility.all;
        allOldAnchors.remove(this.newestAnchor);

        Anchor closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (Anchor anchor : allOldAnchors) {
          final double distance = anchor.pos.distanceTo(playerPos);
          if (distance < closestDistance) {
            closest = anchor;
            closestDistance = distance;
          }
        }

        if (noLongerVisible.contains(this.newestAnchor)
            || (closest != null && noLongerVisible.contains(closest))) { // new anchor
          final Anchor newAnchor = new Anchor(playerPos);
          for (Anchor anchor : noLongerVisible) {
            anchor.connectAnchor(newAnchor);
          }
          for (Anchor anchor : visibleAnchors) {
            if (!noLongerVisible.contains(anchor)) {
              anchor.connectAnchor(newAnchor);
            }
          }
          this.newestAnchor = newAnchor;
          this.graphAnchorCount++;
          this.recordedPointCount++;
          this.graphDirty = true;
          trimIntermediatePoints();
          this.renderSnapshotDirty = true;
        } else { // new point for the last anchor
          final Deque<Vec3d> points = this.newestAnchor.points;
          // don't spam points if we don't move
          if (points.isEmpty()
              || points.peekLast().distanceTo(playerPos) > 0.01) {
            points.addLast(playerPos);
            this.recordedPointCount++;
            trimIntermediatePoints();
            this.renderSnapshotDirty = true;
          }
        }

        final Set<Anchor> nextVisible = new HashSet<>(visibleAnchors);
        if (this.topologyPruned) {
          nextVisible.retainAll(this.currentPath);
          this.topologyPruned = false;
        }
        this.visibleLastTick = nextVisible;
      }
    }

    refreshRenderSnapshotIfNeeded();
  }

  private List<Anchor> getCurrentPath() {
    if (this.rootAnchor == null || this.newestAnchor == null) {
      return Collections.emptyList();
    }
    if (this.graphDirty) {
      this.currentPath = pathFind(this.rootAnchor, this.newestAnchor);
      this.graphDirty = false;
    }
    return this.currentPath;
  }

  private List<Vec3d> getAllPoints(List<Anchor> path, boolean drawIntermediate, int smoothnessValue) {
    final List<Vec3d> points = new ArrayList<>();

    for (int i = 0; i < path.size(); i++) {
      final Anchor anchor = path.get(i);

      points.add(anchor.pos);
      if (drawIntermediate) {
        // if this is the last anchor or this anchor's point list can be linked to the next
        final boolean linkedToNext = i < path.size() - 1
            && !anchor.points.isEmpty()
            && anchor.points.peekLast().distanceTo(path.get(i + 1).pos) < 1;
        if (i == path.size() - 1 || linkedToNext) {
          int point = 0;
          for (Vec3d intermediate : anchor.points) {
            if (point % smoothnessValue == 0) points.add(intermediate);
            point++;
          }
        }
      }
    }

    return points;
  }

  private void trimIntermediatePoints() {
    if (this.recordedPointCount > this.maxpoints.get()) {
      trimStoredTrails(this.maxpoints.get());
    }
  }

  private void trimStoredTrails(int maxPoints) {
    int excess = this.recordedPointCount - maxPoints;
    if (excess <= 0) return;

    excess -= trimOldestTrails(excess);

    if (excess > 0 && this.rootAnchor != null) {
      trimAnchorPoints(this.rootAnchor, new HashSet<>(), excess);
    }
    if (this.recordedPointCount > maxPoints && this.rootAnchor != null) {
      trimGraphHistory(maxPoints);
    }
    this.renderSnapshotDirty = true;
  }

  private int trimOldestTrails(int limit) {
    int removed = 0;
    for (int i = 0; i < this.trails.size() && removed < limit; ) {
      final Trail trail = this.trails.get(i);
      final int remove = Math.min(limit - removed, trail.size());
      if (remove == trail.size()) {
        this.trails.remove(i);
      } else {
        trail.discardOldest(remove);
        i++;
      }
      this.recordedPointCount -= remove;
      removed += remove;
    }
    return removed;
  }

  private void trimGraphHistory(int maxPoints) {
    if (this.rootAnchor == null || this.newestAnchor == null) return;

    final int currentGraphPointCount =
        this.graphAnchorCount + countAnchorPoints(this.rootAnchor, new HashSet<>());
    int historicalPointCount = this.recordedPointCount - currentGraphPointCount;
    final int historicalLimit = Math.max(0, maxPoints - 1);
    if (historicalPointCount > historicalLimit) {
      trimOldestTrails(historicalPointCount - historicalLimit);
      historicalPointCount = this.recordedPointCount - currentGraphPointCount;
    }

    final int available = Math.max(1, maxPoints - Math.max(0, historicalPointCount));
    final int headroom = Math.max(64, available / 10);
    final int target = Math.max(1, available - headroom);
    final List<Anchor> path = new ArrayList<>(getCurrentPath());
    if (path.isEmpty()) {
      path.add(this.newestAnchor);
    }

    final List<Anchor> retainedReverse = new ArrayList<>();
    int remaining = target;
    for (int i = path.size() - 1; i >= 0 && remaining > 0; i--) {
      final Anchor anchor = path.get(i);
      final int allowedPoints = Math.min(anchor.points.size(), Math.max(0, remaining - 1));
      for (int point = anchor.points.size() - allowedPoints; point > 0; point--) {
        anchor.points.removeFirst();
      }
      retainedReverse.add(anchor);
      remaining -= 1 + anchor.points.size();
    }

    Collections.reverse(retainedReverse);
    final List<Anchor> retained = retainedReverse;
    final Anchor retainedRoot = retained.get(0);
    for (Anchor anchor : retained) {
      anchor.connected.clear();
    }
    for (int i = 1; i < retained.size(); i++) {
      retained.get(i - 1).connectAnchor(retained.get(i));
    }

    this.rootAnchor = retainedRoot;
    this.newestAnchor = retained.get(retained.size() - 1);
    this.currentPath = Collections.unmodifiableList(new ArrayList<>(retained));
    this.graphDirty = false;
    this.graphAnchorCount = retained.size();
    this.visibleLastTick = new HashSet<>(this.visibleLastTick);
    this.visibleLastTick.retainAll(retained);
    this.topologyPruned = true;
    this.recordedPointCount = historicalPointCount
        + this.graphAnchorCount
        + countAnchorPoints(this.rootAnchor, new HashSet<>());
  }

  private int trimAnchorPoints(Anchor anchor, Set<Anchor> visited, int limit) {
    if (limit <= 0 || !visited.add(anchor)) return 0;

    final int remove = Math.min(limit, anchor.points.size());
    if (remove > 0) {
      for (int i = 0; i < remove; i++) {
        anchor.points.removeFirst();
      }
      this.recordedPointCount -= remove;
    }

    int removed = remove;
    for (Anchor connected : anchor.connected) {
      if (removed >= limit) break;
      removed += trimAnchorPoints(connected, visited, limit - removed);
    }
    return removed;
  }

  private int countAnchorPoints(Anchor anchor, Set<Anchor> visited) {
    if (!visited.add(anchor)) return 0;
    int count = anchor.points.size();
    for (Anchor connected : anchor.connected) {
      count += countAnchorPoints(connected, visited);
    }
    return count;
  }

  private void refreshRenderSnapshotIfNeeded() {
    final int smoothnessValue = this.smoothness.get();
    final boolean drawIntermediateValue = this.drawIntermediate.get();
    final int maxPointsValue = this.maxpoints.get();
    if (smoothnessValue != this.cachedSmoothness
        || drawIntermediateValue != this.cachedDrawIntermediate
        || maxPointsValue != this.cachedMaxPoints) {
      this.cachedSmoothness = smoothnessValue;
      this.cachedDrawIntermediate = drawIntermediateValue;
      this.cachedMaxPoints = maxPointsValue;
      trimStoredTrails(maxPointsValue);
      this.renderSnapshotDirty = true;
    }
    if (!this.renderSnapshotDirty) return;

    final List<List<Vec3d>> snapshot = new ArrayList<>();
    for (Trail trail : this.trails) {
      if (trail.dimension == this.dimension && trail.size() > 0) {
        snapshot.add(trail.visiblePoints());
      }
    }
    if (this.rootAnchor != null) {
      final List<Vec3d> currentTrail =
          getAllPoints(getCurrentPath(), drawIntermediateValue, smoothnessValue);
      if (!currentTrail.isEmpty()) snapshot.add(Collections.unmodifiableList(currentTrail));
    }
    this.pointsToDraw = Collections.unmodifiableList(snapshot);
    this.renderSnapshotDirty = false;
  }

  private void pushNewTrailAndReset() {
    if (this.rootAnchor == null) return;
    final int currentPointCount = countAnchorPoints(this.rootAnchor, new HashSet<>());
    final int currentGraphPointCount = currentPointCount + this.graphAnchorCount;
    final Trail trail = new Trail(
        this.dimension,
        getAllPoints(getCurrentPath(), this.drawIntermediate.get(), this.smoothness.get())
    );
    this.recordedPointCount -= currentGraphPointCount;
    this.recordedPointCount += trail.size();
    this.trails.add(trail);
    this.graphAnchorCount = 0;
    trimStoredTrails(this.maxpoints.get());

    this.rootAnchor = null;
    this.newestAnchor = null;
    this.currentPath = Collections.emptyList();
    this.graphDirty = true;
    this.visibleLastTick = Collections.emptySet();
    this.renderSnapshotDirty = true;
  }

  @SubscribeEvent
  public void onRender(RenderEvent event) {
    refreshRenderSnapshotIfNeeded();
    BufferBuilder builder = event.getBuffer();

    for (List<Vec3d> path : this.pointsToDraw) {
      builder.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
      for (Vec3d p : path) {
        builder.pos(p.x, p.y, p.z).color(255, 0, 0, 255).endVertex();
      }
      event.getTessellator().draw();
    }
  }

  @SubscribeEvent
  public void onPacketReceived(PacketEvent.Incoming.Pre event) {
    if (!(event.getPacket() instanceof SPacketRespawn)) return;
    MC.addScheduledTask(() -> {
      // This should never happen
      if (this.rootAnchor == null) return;

      this.pushNewTrailAndReset();
    });
  }

  @Override
  public void onLoad() {
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("clear")
        .description("clear points")
        .processor(data -> {
          MC.addScheduledTask(() -> {
            this.rootAnchor = null;
            this.newestAnchor = null;
            this.visibleLastTick = Collections.emptySet();
            this.trails.clear();
            this.currentPath = Collections.emptyList();
            this.recordedPointCount = 0;
            this.graphAnchorCount = 0;
            this.topologyPruned = false;
            this.graphDirty = true;
            this.renderSnapshotDirty = true;
            this.pointsToDraw = Collections.emptyList();
          });
        })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("save")
        .description("Save breadcrumb history to file")
        .requiredArgs(1)
        .processor(data -> {
          MC.addScheduledTask(() -> {
            try {
              trimStoredTrails(this.maxpoints.get());
              List<Trail> trails = new ArrayList<>(this.trails);
              if (this.rootAnchor != null) {
                trails.add(new Trail(
                    this.dimension,
                    getAllPoints(getCurrentPath(), this.drawIntermediate.get(), this.smoothness.get())
                ));
              }

              final Path out = BASE_PATH.resolve(data.getArgumentAsString(0));
              Serialization.serialize(trails, out);
            } catch (IOException ex) {
              Helper.printError(ex.toString());
              ex.printStackTrace();
            }
          });
        })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("load")
        .description("Load a breadcrumb file")
        .requiredArgs(1)
        .processor(data -> {
          MC.addScheduledTask(() -> {
            try {
              this.trails.clear();
              this.rootAnchor = null;
              this.newestAnchor = null;
              this.currentPath = Collections.emptyList();
              this.recordedPointCount = 0;
              this.graphAnchorCount = 0;
              this.topologyPruned = false;
              this.graphDirty = true;
              this.renderSnapshotDirty = true;
              this.pointsToDraw = Collections.emptyList();

              this.trails.addAll(Serialization.deserialize(BASE_PATH.resolve(data.getArgumentAsString(0))));
              for (Trail trail : this.trails) {
                this.recordedPointCount += trail.size();
              }
              trimStoredTrails(this.maxpoints.get());
              this.recording = false;
            } catch (IOException ex) {
              Helper.printError(ex.toString());
              ex.printStackTrace();
            }
          });
        })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("pause")
        .description("Stop recording points")
        .processor(data -> {
          MC.addScheduledTask(() -> {
            this.recording = false;
            this.pushNewTrailAndReset();
          });
        })
        .build();
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("resume")
        .description("Resume recording points")
        .processor(data -> {
          MC.addScheduledTask(() -> {
            this.recording = true;
          });
        })
        .build();
  }

  private enum Serialization {
    ;

    static {
      try {
        if (!Files.exists(BASE_PATH)) Files.createDirectories(BASE_PATH);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private static Trail readTrail(DataInputStream dis) throws IOException {
      final int dim = dis.readInt();
      final int size = dis.readInt(); // number of points
      final List<Vec3d> points = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        points.add(new Vec3d(
            dis.readDouble(),
            dis.readDouble(),
            dis.readDouble()
        ));
      }

      return new Trail(dim, points);
    }

    private static void writeTrail(Trail trail, DataOutputStream dos) throws IOException {
      dos.writeInt(trail.dimension);
      dos.writeInt(trail.size());
      for (int i = 0; i < trail.size(); i++) {
        Vec3d p = trail.get(i);
        dos.writeDouble(p.x);
        dos.writeDouble(p.y);
        dos.writeDouble(p.z);
      }
    }

    static List<Trail> deserialize(Path p) throws IOException {
      try (DataInputStream is = new DataInputStream(Files.newInputStream(p))) {
        final int numTrails = is.readInt();
        final List<Trail> trails = new ArrayList<>(numTrails);
        for (int i = 0; i < numTrails; i++) {
          trails.add(readTrail(is));
        }

        return trails;
      }
    }

    static void serialize(List<Trail> trails, Path p) throws IOException {
      try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(p))) {
        dos.writeInt(trails.size());
        for (Trail t : trails) {
          writeTrail(t, dos);
        }
      }
    }
  }

  // this should be immutable
  /*private static class Trail {
    final int dimension;
    private final Anchor root;
    private final Anchor last;
    final List<Anchor> path;

    Trail(int dim, Anchor root, Anchor last) {
      this.dimension = dim;
      this.root = root;
      this.last = last;
      this.path = Collections.unmodifiableList(pathFind(root, last));
    }
  }*/
  private static class Trail {
    final int dimension;
    final List<Vec3d> points;
    private int firstPoint;

    Trail(int dim, List<Vec3d> path) {
      this.dimension = dim;
      this.points = Collections.unmodifiableList(path);
    }

    int size() {
      return this.points.size() - this.firstPoint;
    }

    Vec3d get(int index) {
      return this.points.get(this.firstPoint + index);
    }

    List<Vec3d> visiblePoints() {
      return this.firstPoint == 0
          ? this.points
          : this.points.subList(this.firstPoint, this.points.size());
    }

    int discardOldest(int count) {
      final int discarded = Math.min(count, size());
      this.firstPoint += discarded;
      return discarded;
    }

  }

  private static class Anchor {
    final Vec3d pos;
    final Deque<Vec3d> points;
    final List<Anchor> connected;

    Anchor(Vec3d pos) {
      this(pos, new ArrayDeque<>(), new ArrayList<>());
    }

    Anchor(Vec3d pos, Deque<Vec3d> points, List<Anchor> connected) {
      this.pos = pos;
      this.points = points;
      this.connected = connected;
    }

    void connectAnchor(Anchor anchor) {
      this.connected.add(anchor);
    }
  }

  private static class AnchorVisibility {
    final Set<Anchor> all;
    final Set<Anchor> visible;

    AnchorVisibility(Set<Anchor> all, Set<Anchor> visible) {
      this.all = all;
      this.visible = visible;
    }
  }

  private static class PathNode {
    final Anchor anchor;
    final double distance;

    PathNode(Anchor anchor, double distance) {
      this.anchor = anchor;
      this.distance = distance;
    }
  }

}
