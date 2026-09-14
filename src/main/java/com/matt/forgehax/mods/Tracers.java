package com.matt.forgehax.mods;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.events.Render2DEvent;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.mobtypes.MobTypeEnum;
import com.matt.forgehax.util.draw.SurfaceBuilder;
import com.matt.forgehax.util.math.AngleHelper;
import com.matt.forgehax.util.math.Plane;
import com.matt.forgehax.util.math.VectorUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

/**
 * Created on 8/6/2017 by fr1kin
 */
@RegisterMod
public class Tracers extends ToggleMod implements Colors {
  public final Setting<Mode> mode =
      getCommandStub()
          .builders()
          .<Mode>newSettingEnumBuilder()
          .name("mode")
          .description("Tracer drawing mode")
          .defaultTo(Mode.ARROWS)
          .build();
  public final Setting<Integer> alpha =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("alpha")
          .description("Alpha value of the tracer color")
          .defaultTo(255)
          .min(0)
          .max(255)
          .build();
  public final Setting<Boolean> antialias =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("antialias")
          .description("Makes lines and triangles more smooth, but may hurt performance")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> players =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("players")
          .description("trace players")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> hostile =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("hostile")
          .description("trace hostile mobs")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> neutral =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("neutral")
          .description("trace neutral mobs")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> friendly =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("friendly")
          .description("trace friendly mobs")
          .defaultTo(true)
          .build();
  private final List<EntityRelations> renderEntities = new ArrayList<>();

  public Tracers() {
    super(Category.RENDER, "Tracers", false, "See where other players are");
  }

  @SubscribeEvent
  public void onUpdate(LocalPlayerUpdateEvent event) {
    renderEntities.clear();
    for (Entity entity : getWorld().entitiesForRendering()) {
      if (!Objects.equals(entity, getLocalPlayer()) && entity instanceof LivingEntity) {
        EntityRelations relations = new EntityRelations(entity);
        if (!relations.getRelationship().equals(MobTypeEnum.INVALID)) {
          renderEntities.add(relations);
        }
      }
    }
  }

  @Override
  protected void onDisabled() {
    renderEntities.clear();
  }

  @SubscribeEvent
  public void onDrawScreen(Render2DEvent event) {
    SurfaceBuilder builder = event.getSurfaceBuilder();
    builder.reset().push().task(SurfaceBuilder::enableBlend).task(SurfaceBuilder::disableTexture2D);

    if (antialias.get()) {
      GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }

    final Mode dm = mode.get();

    final double cx = event.getScreenWidth() / 2.f;
    final double cy = event.getScreenHeight() / 2.f;

    for (EntityRelations er : renderEntities) {
      if (er.isOptionEnabled()) {
        Entity entity = er.getEntity();
        MobTypeEnum relationship = er.getRelationship();

        Vec3 entityPos =
            EntityUtils.getInterpolatedEyePos(entity, event.getPartialTicks());
        Plane screenPos = VectorUtils.toScreen(entityPos);

        Color color = er.getColor().setAlpha(alpha.get());
        builder.color(color.toBuffer());
        builder.translate(0, 0, er.getDepth());

        if (dm.equals(Mode.BOTH) || dm.equals(Mode.ARROWS)) {
          if (!screenPos.isVisible()) {
            // get position on ellipse

            // dimensions of the ellipse
            final double dx = cx - 2;
            final double dy = cy - 20;

            // ellipse = x^2/a^2 + y^2/b^2 = 1
            // e = (pos - C) / d
            //  C = center vector
            //  d = dimensions
            double ex = (screenPos.getX() - cx) / dx;
            double ey = (screenPos.getY() - cy) / dy;

            // normalize
            // n = u/|u|
            double m = Math.abs(Math.sqrt(ex * ex + ey * ey));
            double nx = ex / m;
            double ny = ey / m;

            // scale
            // p = C + dot(n,d)
            double x = cx + nx * dx;
            double y = cy + ny * dy;

            // --------------------
            // now rotate triangle

            // point - center
            // w = <px - cx, py - cy>
            double wx = x - cx;
            double wy = y - cy;

            // u = <w, 0>
            double ux = event.getScreenWidth();
            double uy = 0.D;

            // |u|
            double mu = Math.sqrt(ux * ux + uy * uy);
            // |w|
            double mw = Math.sqrt(wx * wx + wy * wy);

            // theta = dot(u,w)/(|u|*|w|)
            double ang = Math.toDegrees(Math.acos((ux * wx + uy * wy) / (mu * mw)));

            // don't allow NaN angles
            if (Double.isNaN(ang)) {
              ang = 0;
            }

            // invert
            if (y < cy) {
              ang *= -1;
            }

            // normalize
            ang = AngleHelper.normalizeInDegrees(ang);

            // --------------------

            int size = relationship.equals(MobTypeEnum.PLAYER) ? 8 : 5;

            builder.push()
                .translate(x, y, 0)
                .rotate(ang, 0, 0, 1)
                .color(color.toBuffer())
                .beginPolygon()
                .vertex(0, 0)
                .vertex(-size, -size)
                .vertex(-size, size)
                .end()
                .pop();
          }
        }

        if (dm.equals(Mode.BOTH) || dm.equals(Mode.LINES)) {
          builder.beginLines()
              .vertex(cx, cy)
              .vertex(screenPos.getX(), screenPos.getY())
              .end();
        }

        builder.translate(0, 0, -er.getDepth());
      }
    }

    builder.task(SurfaceBuilder::enableTexture2D)
        .task(SurfaceBuilder::disableBlend)
        .pop();

    GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    GL11.glDisable(GL11.GL_LINE_SMOOTH);
  }

  enum Mode {
    ARROWS,
    LINES,
    BOTH,
  }

  private class EntityRelations implements Comparable<EntityRelations> {

    private final Entity entity;
    private final MobTypeEnum relationship;

    public EntityRelations(Entity entity) {
      Objects.requireNonNull(entity);
      this.entity = entity;
      this.relationship = EntityUtils.getRelationship(entity);
    }

    public Entity getEntity() {
      return entity;
    }

    public MobTypeEnum getRelationship() {
      return relationship;
    }

    public Color getColor() {
      switch (relationship) {
        case PLAYER:
          return YELLOW;
        case HOSTILE:
          return RED;
        case NEUTRAL:
          return BLUE;
        default:
          return GREEN;
      }
    }

    public float getDepth() {
      switch (relationship) {
        case PLAYER:
          return 15.f;
        case HOSTILE:
          return 10.f;
        case NEUTRAL:
          return 5.f;
        default:
          return 0.f;
      }
    }

    public boolean isOptionEnabled() {
      switch (relationship) {
        case PLAYER:
          return players.get();
        case HOSTILE:
          return hostile.get();
        case NEUTRAL:
          return neutral.get();
        default:
          return friendly.get();
      }
    }

    @Override
    public int compareTo(EntityRelations o) {
      return getRelationship().compareTo(o.getRelationship());
    }
  }
}
