package net.minecraft.client.model.geom.builders;

public final class CubeDeformation {
   public static final CubeDeformation f_171458_ = new CubeDeformation(0.0F);
   public final float grow;

   public CubeDeformation(float grow) {
      this.grow = grow;
   }

   public CubeDeformation m_171469_(float more) {
      return new CubeDeformation(grow + more);
   }
}
