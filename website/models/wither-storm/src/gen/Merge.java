package gen;

import java.util.*;

/**
 * Greedy-merges a voxel body into fewer, larger boxes of the same block: the high-detail masses are
 * thousands of unit cubes, and every box costs the server one display entity.
 */
final class Merge {

   /** Longest run a merged box may have on any axis, so a block texture is never stretched far. */
   static final int MAX_RUN = 6;

   static List<Gen.Box> greedy(List<Gen.Box> boxes) {
      for (Gen.Box b : boxes) {
         if (!whole(b.x()) || !whole(b.y()) || !whole(b.z()) || !whole(b.w()) || !whole(b.h()) || !whole(b.d())) {
            return boxes;
         }
      }
      String path = boxes.isEmpty() ? "" : boxes.get(0).path();
      Map<String, Set<Long>> byMaterial = new LinkedHashMap<>();
      Map<String, Boolean> glow = new HashMap<>();
      for (Gen.Box b : boxes) {
         String key = b.material() + (b.glow() ? "+" : "");
         glow.put(key, b.glow());
         Set<Long> cells = byMaterial.computeIfAbsent(key, k -> new HashSet<>());
         for (int x = (int) b.x(); x < b.x() + b.w(); x++)
            for (int y = (int) b.y(); y < b.y() + b.h(); y++)
               for (int z = (int) b.z(); z < b.z() + b.d(); z++)
                  cells.add(pack(x, y, z));
      }
      List<Gen.Box> out = new ArrayList<>();
      for (Map.Entry<String, Set<Long>> e : byMaterial.entrySet()) {
         Set<Long> left = new HashSet<>(e.getValue());
         List<Long> order = new ArrayList<>(left);
         Collections.sort(order);
         String material = e.getKey().replace("+", "");
         for (long cell : order) {
            if (!left.contains(cell)) continue;
            int x = ux(cell), y = uy(cell), z = uz(cell);
            int w = 1, h = 1, d = 1;
            while (w < MAX_RUN && left.contains(pack(x + w, y, z))) w++;
            while (h < MAX_RUN && row(left, x, y + h, z, w)) h++;
            while (d < MAX_RUN && slab(left, x, y, z + d, w, h)) d++;
            for (int i = 0; i < w; i++)
               for (int j = 0; j < h; j++)
                  for (int k = 0; k < d; k++)
                     left.remove(pack(x + i, y + j, z + k));
            out.add(new Gen.Box(path, x, y, z, w, h, d, material, glow.get(e.getKey())));
         }
      }
      return out.size() < boxes.size() ? out : boxes;
   }

   private static boolean row(Set<Long> cells, int x, int y, int z, int w) {
      for (int i = 0; i < w; i++) if (!cells.contains(pack(x + i, y, z))) return false;
      return true;
   }

   private static boolean slab(Set<Long> cells, int x, int y, int z, int w, int h) {
      for (int j = 0; j < h; j++) if (!row(cells, x, y + j, z, w)) return false;
      return true;
   }

   private static boolean whole(float v) {
      return Math.abs(v - Math.round(v)) < 1e-4;
   }

   private static long pack(int x, int y, int z) {
      return ((long) (x + 4096) << 40) | ((long) (y + 4096) << 20) | (z + 4096);
   }

   private static int ux(long c) { return (int) (c >> 40) - 4096; }
   private static int uy(long c) { return (int) ((c >> 20) & 0xFFFFF) - 4096; }
   private static int uz(long c) { return (int) (c & 0xFFFFF) - 4096; }

   private Merge() {
   }
}
