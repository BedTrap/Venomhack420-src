package venomhack.enums;

public enum RenderShape {
   CUBOID("Cuboid"),
   OCTAHEDRON("Octahedron"),
   CORNER_PYRAMIDS("Corner Pyramids");

   private final String title;

   private RenderShape(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
