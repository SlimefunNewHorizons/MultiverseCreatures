package gen;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import nonamecrackers2.witherstormmod.client.renderer.entity.model.witherstorm.mass.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Builds the five Wither Storm forms the plugin uses from the mod's own model code, and writes each
 * one as the plugin's model text: the part tree, every box with the block it is drawn with, and the
 * head and tentacle settings the animation needs.
 */
public final class Gen {

   static BufferedImage texture, emissive;

   record Group(String kind, String path, float scale, float rotX) {}
   record Head(int index, String path, float animOffset, float beamY, float pivotX, float pivotY, float pivotZ) {}
   record Tentacle(String path, float scale, float speed, float offset, float xRot, float yRot, float xAng, float yAng, float reach, float rotX) {}

   static final class Form {
      final String name;
      final PartDefinition root = new PartDefinition("root", List.of(), PartPose.f_171404_);
      final List<Group> groups = new ArrayList<>();
      final List<Head> heads = new ArrayList<>();
      final List<Tentacle> tentacles = new ArrayList<>();
      boolean mergeMass;

      Form(String name) {
         this.name = name;
         root.m_171599_("mass", CubeListBuilder.m_171558_(), PartPose.f_171404_);
         root.m_171599_("lowResMass", CubeListBuilder.m_171558_(), PartPose.f_171404_);
         root.m_171599_("tentacles", CubeListBuilder.m_171558_(), PartPose.f_171404_);
         root.m_171599_("heads", CubeListBuilder.m_171558_(), PartPose.f_171404_);
      }

      void tentacle(String name, int[] lengths, PartPose pose, float scale, float speed, float offset,
                    float xRot, float yRot, float xAng, float yAng, float reach, float rotX) {
         Parts.tentacle(root.m_171597_("tentacles").m_171599_(name, CubeListBuilder.m_171558_(), PartPose.f_171404_), lengths, pose);
         tentacles.add(new Tentacle("tentacles/" + name + "/base", scale, speed, offset, xRot, yRot, xAng, yAng, reach, rotX));
      }

      void threeHeads(float scale) {
         PartPose[] at = {PartPose.m_171419_(-22.0F, -65.0F, -40.0F), PartPose.m_171419_(0.0F, -32.0F, -23.0F), PartPose.m_171419_(32.0F, -60.0F, -24.0F)};
         int[] index = {2, 0, 1};
         float[] anim = {100, 0, 175};
         for (int i = 0; i < 3; i++) {
            Parts.head(root.m_171597_("heads").m_171599_("head" + i, CubeListBuilder.m_171558_(), at[i]));
            heads.add(new Head(index[i], "heads/head" + i, anim[i], 8.0F, -4.0F, 0.325F, 0.0F));
            groups.add(new Group("head", "heads/head" + i, scale, 0));
         }
      }
   }

   static float rad(double deg) {
      return (float) Math.toRadians(deg);
   }

   static List<Form> forms() {
      CubeDeformation def = CubeDeformation.f_171458_;
      List<Form> forms = new ArrayList<>();

      // Phase 1: the Wither with its command block and the first lump of mass on its back.
      Form hunchback = new Form("hunchback");
      Parts.witherBase(hunchback.root, def, true, true, true);
      HunchbackBodyModel.createBodyModel(hunchback.root, 1.0F);
      hunchback.groups.add(new Group("base", "witherBase", 1, 0));
      hunchback.groups.add(new Group("mass", "mass", 1, 0));
      forms.add(hunchback);

      // Phase 2: the mass has swallowed the middle head and grown a jaw of its own.
      Form growing = new Form("growing");
      Parts.head(growing.root.m_171597_("heads").m_171599_("head0", CubeListBuilder.m_171558_(), PartPose.f_171404_));
      growing.heads.add(new Head(0, "heads/head0", 0, 34.0F, -19.0F, 1.85F, 0.0F));
      growing.groups.add(new Group("head", "heads/head0", 0.7F, 0));
      Parts.witherBase(growing.root, def, false, true, true);
      GrowingHunchbackMassModel.createMassModel(growing.root, 1.0F);
      growing.groups.add(new Group("base", "witherBase", 1, 0));
      growing.groups.add(new Group("mass", "mass", 1.001F, 0));
      forms.add(growing);

      // Phase 3: the swollen hunchback, its first three tentacles hanging from the mass.
      Form pregnant = new Form("pregnant");
      Parts.head(pregnant.root.m_171597_("heads").m_171599_("head0", CubeListBuilder.m_171558_(), PartPose.f_171404_));
      pregnant.heads.add(new Head(0, "heads/head0", 0, 34.0F, -19.0F, 1.85F, 0.0F));
      pregnant.groups.add(new Group("head", "heads/head0", 0.7F, 0));
      Parts.witherBase(pregnant.root, def, false, false, false);
      PregnantHunchbackBodyModel.createBodyModel(pregnant.root, 1.0F);
      pregnant.groups.add(new Group("base", "witherBase", 1, 0));
      pregnant.groups.add(new Group("mass", "mass", 1, 20));
      pregnant.tentacle("tentacle0", new int[]{18, 24, 24, 24, 24, 32}, PartPose.m_171419_(0, 0, 20), 0.5F, 0.5F, 0, 0, 4.14F, -0.17444445F, 0.3488889F, 2.0F, 20);
      pregnant.tentacle("tentacle1", new int[]{18, 24, 24, 24, 24, 32}, PartPose.m_171419_(0, 0, 20), 0.5F, 0.5F, 10, 9.859601F, 11.775001F, 0.17444445F, 0.3488889F, 2.15F, 20);
      pregnant.tentacle("tentacle2", new int[]{18, 24, 24, 24, 24, 32}, PartPose.m_171419_(0, 20, 20), 0.5F, 0.5F, 32, 0.785F, 3.14F, 0.5233334F, -0.17444445F, 1.85F, 20);
      forms.add(pregnant);

      // Phase 4: the Destroyer, three heads on a flying mass. The plugin flies the mod's low-detail
      // mass (33 boxes) unless high-detail is switched on, which draws the full 401-voxel one.
      forms.add(destroyer("destroyer", false));
      forms.add(destroyer("destroyer-detailed", true));

      // Phase 5: the Devourer, its mass a vast cradle under the three heads.
      Form devourer = new Form("devourer");
      devourer.threeHeads(3.0F);
      LowResDevourerBodyModel.createBodyModel(devourer.root, 0.3F);
      devourer.groups.add(new Group("mass", "lowResMass", 10, 0));
      devourer.tentacle("tentacle0", new int[]{18, 24, 24, 28, 28, 32}, PartPose.m_171419_(-20, -25, 5), 3, 0.2F, 0, rad(90), rad(90), -0.3925F, 0.17444445F, 2.0F, 0);
      devourer.tentacle("tentacle1", new int[]{18, 24, 24, 24, 28, 38}, PartPose.m_171419_(20, -27.5F, 7), 3, 0.4F, 8, rad(90), rad(270), -0.3925F, -0.17444445F, 1.0F, 0);
      devourer.tentacle("tentacle2", new int[]{18, 24, 24, 28, 32, 28}, PartPose.m_171419_(-10, -30, -10), 3, 0.2F, 16, rad(100), 0, -0.3925F, 0.2616667F, 1.5F, 0);
      devourer.tentacle("tentacle3", new int[]{18, 18, 24, 24, 24, 28}, PartPose.m_171419_(8, -34, -6), 3, 0.2F, 9, rad(90), rad(320), -0.3925F, -0.13083334F, 1.75F, 0);
      devourer.tentacle("tentacle4", new int[]{18, 18, 24, 28, 32, 32}, PartPose.m_171419_(-8, -25, 16), 3, 0.2F, 12, rad(70), rad(120), -0.3925F, 0.2616667F, 2.0F, 0);
      devourer.tentacle("tentacle5", new int[]{18, 20, 26, 28, 32, 28}, PartPose.m_171419_(10, -23, 19), 3, 0.4F, 20, rad(70), rad(220), -0.44857144F, -0.2616667F, 1.5F, 0);
      devourer.tentacle("tentacle6", new int[]{18, 20, 26, 28, 28, 24}, PartPose.m_171419_(-2, 0, 0), 3, 0.15F, 24, rad(90), rad(45), 0.2616667F, 0.3925F, 2.0F, 0);
      devourer.tentacle("tentacleLarge0", new int[]{16, 20, 24, 28, 32, 28}, PartPose.m_171419_(-24, -28, 0), 4.5F, 0.25F, 80, rad(120), rad(-100), 0.2616667F, 0.3925F, 2.0F, 0);
      devourer.tentacle("tentacleLarge1", new int[]{20, 20, 24, 24, 28, 32}, PartPose.m_171419_(28, -28, 2), 4.5F, 0.25F, 35, rad(100), rad(120), 0.3925F, -0.2616667F, 2.0F, 0);
      forms.add(devourer);
      return forms;
   }

   static Form destroyer(String name, boolean detailed) {
      Form destroyer = new Form(name);
      destroyer.threeHeads(3.0F);
      if (detailed) {
         DestroyerBodyModel.createBodyModel(destroyer.root, 0.2F);
         destroyer.groups.add(new Group("mass", "mass", 10, 0));
         destroyer.mergeMass = true;
      } else {
         LowResDestroyerBodyModel.createBodyModel(destroyer.root, 0.3F);
         destroyer.groups.add(new Group("mass", "lowResMass", 10, 0));
      }
      destroyer.tentacle("tentacle0", new int[]{23, 28, 28, 28, 32, 32}, PartPose.m_171419_(-10, -30, 0), 2, 0.6F, 0, 0.3925F, 1.57F, -0.19625F, -0.19625F, 2, 0);
      destroyer.tentacle("tentacle1", new int[]{23, 28, 28, 28, 32, 32}, PartPose.m_171419_(30, -115, -10), 2, 0.6F, 20, -0.628F, -0.3925F, 0.3925F, -0.2616667F, 2, 0);
      destroyer.tentacle("tentacle2", new int[]{23, 28, 28, 28, 32, 32}, PartPose.m_171419_(10, -40, 10), 2, 0.6F, 10, 0, -1.7444445F, 0.3925F, -0.19625F, 2, 0);
      destroyer.tentacle("tentacle3", new int[]{23, 28, 28, 28, 32, 32}, PartPose.m_171419_(-50, -100, 0), 2, 0.6F, 30, 0, 1.256F, -0.5233334F, 0.3925F, 2, 0);
      destroyer.tentacle("tentacle4", new int[]{23, 28, 28, 28, 32, 32}, PartPose.m_171419_(-10, -95, 15), 2, 0.6F, 40, -1.0466667F, -0.19625F, 0.2616667F, 0.098125F, 2, 0);
      return destroyer;
   }

   // ------------------------------------------------------------------ materials

   /** The block a box is drawn with, chosen from the texel colour its front face shows. */
   static String material(String path, CubeListBuilder.Cube cube) {
      if (path.endsWith("ribcageExtension/block")) return "COMMAND_BLOCK";
      if (path.endsWith("center_head") || path.endsWith("right_head") || path.endsWith("left_head")) return "SKULL";
      if (path.endsWith("Teeth")) return "WHITE_CONCRETE";
      if (path.endsWith("upperJaw") && cube.u() == 4 && cube.v() == 13) return "MAGENTA_CONCRETE";
      int[] rgb = sample(texture, cube);
      return nearest(rgb);
   }

   static boolean glows(String path, CubeListBuilder.Cube cube) {
      if (path.endsWith("Teeth")) return true;
      if (path.endsWith("upperJaw") && cube.u() == 4 && cube.v() == 13) return true;
      if (path.endsWith("ribcageExtension/block")) return true;
      int[] e = sample(emissive, cube);
      return e[3] > 96;
   }

   /** Mean colour (and alpha) of the box's front face on the 160 x 160 texture. */
   static int[] sample(BufferedImage image, CubeListBuilder.Cube c) {
      float s = c.uScale();
      int u0 = (int) Math.floor(c.u() + c.d() * s), v0 = (int) Math.floor(c.v() + c.d() * s);
      int w = Math.max(1, Math.round(c.w() * s)), h = Math.max(1, Math.round(c.h() * s));
      long r = 0, g = 0, b = 0, a = 0, n = 0;
      for (int x = u0; x < u0 + w; x++) {
         for (int y = v0; y < v0 + h; y++) {
            if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) continue;
            int argb = image.getRGB(x, y);
            int alpha = argb >>> 24;
            a += alpha;
            if (alpha < 16) continue;
            r += (argb >> 16) & 255;
            g += (argb >> 8) & 255;
            b += argb & 255;
            n++;
         }
      }
      int area = Math.max(1, w * h);
      return n == 0 ? new int[]{0, 0, 0, (int) (a / area)} : new int[]{(int) (r / n), (int) (g / n), (int) (b / n), (int) (a / area)};
   }

   static final Map<String, int[]> PALETTE = new LinkedHashMap<>();
   static {
      PALETTE.put("BLACK_CONCRETE", new int[]{8, 10, 15});
      PALETTE.put("OBSIDIAN", new int[]{20, 18, 30});
      PALETTE.put("CRYING_OBSIDIAN", new int[]{33, 10, 60});
      PALETTE.put("BLACKSTONE", new int[]{42, 35, 41});
      PALETTE.put("POLISHED_BLACKSTONE", new int[]{53, 48, 56});
      PALETTE.put("GRAY_CONCRETE", new int[]{54, 57, 61});
      PALETTE.put("PURPLE_CONCRETE", new int[]{100, 32, 156});
      PALETTE.put("MAGENTA_CONCRETE", new int[]{169, 48, 159});
      PALETTE.put("AMETHYST_BLOCK", new int[]{133, 97, 191});
      PALETTE.put("LIGHT_GRAY_CONCRETE", new int[]{125, 125, 115});
      PALETTE.put("WHITE_CONCRETE", new int[]{207, 213, 214});
   }

   static String nearest(int[] rgb) {
      String best = "BLACK_CONCRETE";
      double bestDistance = Double.MAX_VALUE;
      for (Map.Entry<String, int[]> e : PALETTE.entrySet()) {
         int[] p = e.getValue();
         double d = Math.pow(rgb[0] - p[0], 2) + Math.pow(rgb[1] - p[1], 2) + Math.pow(rgb[2] - p[2], 2);
         if (d < bestDistance) {
            bestDistance = d;
            best = e.getKey();
         }
      }
      return best;
   }

   // ------------------------------------------------------------------ writing

   record Box(String path, float x, float y, float z, float w, float h, float d, String material, boolean glow) {}

   static List<Box> boxes(String path, PartDefinition part, boolean merge) {
      List<Box> out = new ArrayList<>();
      for (CubeListBuilder.Cube c : part.cubes) {
         float g = c.grow();
         out.add(new Box(path, c.x() - g, c.y() - g, c.z() - g, c.w() + 2 * g, c.h() + 2 * g, c.d() + 2 * g,
               material(path, c), glows(path, c)));
      }
      List<Box> result = merge ? Merge.greedy(out) : out;
      return path.equals("mass") || path.equals("lowResMass") ? speckle(result) : result;
   }

   /**
    * The mod draws its mass from one texel, then lights spots of it with an emissive decal; a body of
    * one block would read as a flat silhouette, so a few boxes take the neighbouring dark blocks and a
    * few the glowing crying obsidian of those lit spots.
    */
   static List<Box> speckle(List<Box> boxes) {
      List<Box> out = new ArrayList<>();
      for (Box b : boxes) {
         int hash = Objects.hash(b.x, b.y, b.z, b.w, b.h, b.d);
         hash ^= hash >>> 16;
         hash *= 0x45d9f3b;
         hash ^= hash >>> 16;
         int roll = Math.floorMod(hash, 100);
         String material = b.material;
         boolean glow = b.glow;
         if (roll < 7) {
            material = "CRYING_OBSIDIAN";
            glow = true;
         } else if (roll < 25) {
            material = material.equals("OBSIDIAN") ? "BLACK_CONCRETE" : "OBSIDIAN";
         }
         out.add(new Box(b.path, b.x, b.y, b.z, b.w, b.h, b.d, material, glow));
      }
      return out;
   }

   static void walk(String path, PartDefinition part, StringBuilder parts, StringBuilder boxes, Form form, int[] count) {
      PartPose p = part.pose;
      parts.append(String.format(Locale.ROOT, "part %s %s %s %s %s %s %s%n", path, f(p.x), f(p.y), f(p.z), f(p.xRot), f(p.yRot), f(p.zRot)));
      boolean merge = form.mergeMass && (path.equals("mass") || path.equals("lowResMass"));
      for (Box b : boxes(path, part, merge)) {
         boxes.append(String.format(Locale.ROOT, "box %s %s %s %s %s %s %s %s %d%n", path, f(b.x), f(b.y), f(b.z), f(b.w), f(b.h), f(b.d), b.material, b.glow ? 1 : 0));
         count[0]++;
      }
      for (Map.Entry<String, PartDefinition> e : part.children.entrySet()) {
         walk(path.isEmpty() ? e.getKey() : path + "/" + e.getKey(), e.getValue(), parts, boxes, form, count);
      }
   }

   static String f(float v) {
      String s = String.format(Locale.ROOT, "%.5f", v);
      s = s.replaceAll("0+$", "");
      return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
   }

   public static void main(String[] args) throws IOException {
      texture = ImageIO.read(new File(args[0]));
      emissive = ImageIO.read(new File(args[1]));
      Path out = Path.of(args[2]);
      Files.createDirectories(out);
      for (Form form : forms()) {
         StringBuilder text = new StringBuilder();
         text.append("# Wither Storm form '").append(form.name).append("', generated from Cracker's Wither Storm Mod 4.2.1 by tools/wither-storm.\n");
         for (Group g : form.groups) {
            text.append(String.format(Locale.ROOT, "group %s %s %s %s%n", g.kind, g.path, f(g.scale), f(g.rotX)));
         }
         for (Head h : form.heads) {
            text.append(String.format(Locale.ROOT, "head %d %s %s %s %s %s %s%n", h.index, h.path, f(h.animOffset), f(h.beamY), f(h.pivotX), f(h.pivotY), f(h.pivotZ)));
         }
         for (Tentacle t : form.tentacles) {
            text.append(String.format(Locale.ROOT, "tentacle %s %s %s %s %s %s %s %s %s %s%n", t.path, f(t.scale), f(t.speed), f(t.offset), f(t.xRot), f(t.yRot), f(t.xAng), f(t.yAng), f(t.reach), f(t.rotX)));
         }
         StringBuilder parts = new StringBuilder(), boxes = new StringBuilder();
         int[] count = {0};
         for (Map.Entry<String, PartDefinition> e : form.root.children.entrySet()) {
            walk(e.getKey(), e.getValue(), parts, boxes, form, count);
         }
         text.append(parts).append(boxes);
         Files.writeString(out.resolve(form.name + ".txt"), text);
         System.out.println(form.name + ": " + count[0] + " boxes");
      }
   }

   private Gen() {
   }
}
