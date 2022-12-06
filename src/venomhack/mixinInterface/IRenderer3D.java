package venomhack.mixinInterface;

import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;

public interface IRenderer3D {
   void fancyThing(double var1, double var3, double var5, double var7, double var9, double var11, Color var13, Color var14, ShapeMode var15, double var16);

   void triangle(
      double var1,
      double var3,
      double var5,
      double var7,
      double var9,
      double var11,
      double var13,
      double var15,
      double var17,
      Color var19,
      Color var20,
      Color var21
   );

   void octahedron(double var1, double var3, double var5, double var7, double var9, double var11, Color var13, Color var14, ShapeMode var15);
}
