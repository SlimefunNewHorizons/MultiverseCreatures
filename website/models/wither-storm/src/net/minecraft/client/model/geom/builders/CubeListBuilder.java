package net.minecraft.client.model.geom.builders;

import java.util.ArrayList;
import java.util.List;

public final class CubeListBuilder {
   public record Cube(float x, float y, float z, float w, float h, float d, float grow, int u, int v, float uScale) {
   }

   public final List<Cube> cubes = new ArrayList<>();
   private int u, v;

   public static CubeListBuilder m_171558_() {
      return new CubeListBuilder();
   }

   public CubeListBuilder m_171514_(int u, int v) {
      this.u = u;
      this.v = v;
      return this;
   }

   public CubeListBuilder m_171480_() {
      return this;
   }

   public CubeListBuilder m_171555_(boolean mirror) {
      return this;
   }

   public CubeListBuilder m_171506_(float x, float y, float z, float w, float h, float d, boolean mirror) {
      cubes.add(new Cube(x, y, z, w, h, d, 0, u, v, 1));
      return this;
   }

   public CubeListBuilder m_171488_(float x, float y, float z, float w, float h, float d, CubeDeformation def) {
      cubes.add(new Cube(x, y, z, w, h, d, def.grow, u, v, 1));
      return this;
   }

   public CubeListBuilder m_171496_(float x, float y, float z, float w, float h, float d, CubeDeformation def, float us, float vs) {
      cubes.add(new Cube(x, y, z, w, h, d, def.grow, u, v, us));
      return this;
   }

   public CubeListBuilder m_171481_(float x, float y, float z, float w, float h, float d) {
      cubes.add(new Cube(x, y, z, w, h, d, 0, u, v, 1));
      return this;
   }
}
