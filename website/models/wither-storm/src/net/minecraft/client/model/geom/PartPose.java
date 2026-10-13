package net.minecraft.client.model.geom;

public final class PartPose {
   public static final PartPose f_171404_ = new PartPose(0, 0, 0, 0, 0, 0);
   public final float x, y, z, xRot, yRot, zRot;

   private PartPose(float x, float y, float z, float xRot, float yRot, float zRot) {
      this.x = x; this.y = y; this.z = z; this.xRot = xRot; this.yRot = yRot; this.zRot = zRot;
   }

   public static PartPose m_171419_(float x, float y, float z) {
      return new PartPose(x, y, z, 0, 0, 0);
   }

   public static PartPose m_171423_(float x, float y, float z, float xRot, float yRot, float zRot) {
      return new PartPose(x, y, z, xRot, yRot, zRot);
   }
}
