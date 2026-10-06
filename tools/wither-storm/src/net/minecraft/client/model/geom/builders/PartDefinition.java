package net.minecraft.client.model.geom.builders;

import net.minecraft.client.model.geom.PartPose;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PartDefinition {
   public final String name;
   public final PartPose pose;
   public final List<CubeListBuilder.Cube> cubes;
   public final Map<String, PartDefinition> children = new LinkedHashMap<>();

   public PartDefinition(String name, List<CubeListBuilder.Cube> cubes, PartPose pose) {
      this.name = name;
      this.cubes = cubes;
      this.pose = pose;
   }

   public PartDefinition m_171599_(String name, CubeListBuilder builder, PartPose pose) {
      PartDefinition child = new PartDefinition(name, new ArrayList<>(builder.cubes), pose);
      children.put(name, child);
      return child;
   }

   public PartDefinition m_171597_(String name) {
      PartDefinition child = children.get(name);
      if (child == null) throw new IllegalArgumentException("no child " + name + " in " + this.name);
      return child;
   }
}
