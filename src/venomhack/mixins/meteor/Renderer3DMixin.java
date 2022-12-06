package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.renderer.Mesh;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import venomhack.mixinInterface.IMesh;
import venomhack.mixinInterface.IRenderer3D;

@Mixin(
   value = {Renderer3D.class},
   remap = false
)
public abstract class Renderer3DMixin implements IRenderer3D {
   @Shadow
   @Final
   public Mesh triangles;

   @Shadow
   public abstract void line(double var1, double var3, double var5, double var7, double var9, double var11, Color var13);

   @Unique
   @Override
   public void fancyThing(double x1, double y1, double z1, double x2, double y2, double z2, Color sideColor, Color lineColor, ShapeMode mode, double dist) {
      double actualXDelta = (x2 - x1) * 0.5 - dist;
      double actualYDelta = (y2 - y1) * 0.5 - dist;
      double actualZDelta = (z2 - z1) * 0.5 - dist;
      if (mode.sides()) {
         this.triangle(x1, y1, z1, x1 + actualXDelta, y1, z1, x1, y1 + actualYDelta, z1, sideColor);
         this.triangle(x1, y1, z1, x1, y1, z1 + actualZDelta, x1, y1 + actualYDelta, z1, sideColor);
         this.triangle(x1, y1, z1, x1 + actualXDelta, y1, z1, x1, y1, z1 + actualZDelta, sideColor);
         this.triangle(x1 + actualXDelta, y1, z1, x1, y1 + actualYDelta, z1, x1, y1, z1 + actualZDelta, sideColor);
         this.triangle(x2, y1, z1, x2 - actualXDelta, y1, z1, x2, y1 + actualYDelta, z1, sideColor);
         this.triangle(x2, y1, z1, x2, y1, z1 + actualZDelta, x2, y1 + actualYDelta, z1, sideColor);
         this.triangle(x2, y1, z1, x2 - actualXDelta, y1, z1, x2, y1, z1 + actualZDelta, sideColor);
         this.triangle(x2 - actualXDelta, y1, z1, x2, y1 + actualYDelta, z1, x2, y1, z1 + actualZDelta, sideColor);
         this.triangle(x2, y1, z2, x2 - actualXDelta, y1, z2, x2, y1 + actualYDelta, z2, sideColor);
         this.triangle(x2, y1, z2, x2, y1, z2 - actualZDelta, x2, y1 + actualYDelta, z2, sideColor);
         this.triangle(x2, y1, z2, x2 - actualXDelta, y1, z2, x2, y1, z2 - actualZDelta, sideColor);
         this.triangle(x2 - actualXDelta, y1, z2, x2, y1 + actualYDelta, z2, x2, y1, z2 - actualZDelta, sideColor);
         this.triangle(x1, y1, z2, x1 + actualXDelta, y1, z2, x1, y1 + actualYDelta, z2, sideColor);
         this.triangle(x1, y1, z2, x1, y1, z2 - actualZDelta, x1, y1 + actualYDelta, z2, sideColor);
         this.triangle(x1, y1, z2, x1 + actualXDelta, y1, z2, x1, y1, z2 - actualZDelta, sideColor);
         this.triangle(x1 + actualXDelta, y1, z2, x1, y1 + actualYDelta, z2, x1, y1, z2 - actualZDelta, sideColor);
         this.triangle(x1, y2, z1, x1 + actualXDelta, y2, z1, x1, y2 - actualYDelta, z1, sideColor);
         this.triangle(x1, y2, z1, x1, y2, z1 + actualZDelta, x1, y2 - actualYDelta, z1, sideColor);
         this.triangle(x1, y2, z1, x1 + actualXDelta, y2, z1, x1, y2, z1 + actualZDelta, sideColor);
         this.triangle(x1 + actualXDelta, y2, z1, x1, y2 - actualYDelta, z1, x1, y2, z1 + actualZDelta, sideColor);
         this.triangle(x2, y2, z1, x2 - actualXDelta, y2, z1, x2, y2 - actualYDelta, z1, sideColor);
         this.triangle(x2, y2, z1, x2, y2, z1 + actualZDelta, x2, y2 - actualYDelta, z1, sideColor);
         this.triangle(x2, y2, z1, x2 - actualXDelta, y2, z1, x2, y2, z1 + actualZDelta, sideColor);
         this.triangle(x2 - actualXDelta, y2, z1, x2, y2 - actualYDelta, z1, x2, y2, z1 + actualZDelta, sideColor);
         this.triangle(x2, y2, z2, x2 - actualXDelta, y2, z2, x2, y2 - actualYDelta, z2, sideColor);
         this.triangle(x2, y2, z2, x2, y2, z2 - actualZDelta, x2, y2 - actualYDelta, z2, sideColor);
         this.triangle(x2, y2, z2, x2 - actualXDelta, y2, z2, x2, y2, z2 - actualZDelta, sideColor);
         this.triangle(x2 - actualXDelta, y2, z2, x2, y2 - actualYDelta, z2, x2, y2, z2 - actualZDelta, sideColor);
         this.triangle(x1, y2, z2, x1 + actualXDelta, y2, z2, x1, y2 - actualYDelta, z2, sideColor);
         this.triangle(x1, y2, z2, x1, y2, z2 - actualZDelta, x1, y2 - actualYDelta, z2, sideColor);
         this.triangle(x1, y2, z2, x1 + actualXDelta, y2, z2, x1, y2, z2 - actualZDelta, sideColor);
         this.triangle(x1 + actualXDelta, y2, z2, x1, y2 - actualYDelta, z2, x1, y2, z2 - actualZDelta, sideColor);
      }

      if (mode.lines()) {
         this.line(x1, y1, z1, x1 + actualXDelta, y1, z1, lineColor);
         this.line(x1, y1, z1, x1, y1 + actualYDelta, z1, lineColor);
         this.line(x1, y1, z1, x1, y1, z1 + actualZDelta, lineColor);
         this.line(x1 + actualXDelta, y1, z1, x1, y1 + actualYDelta, z1, lineColor);
         this.line(x1 + actualXDelta, y1, z1, x1, y1, z1 + actualZDelta, lineColor);
         this.line(x1, y1 + actualYDelta, z1, x1, y1, z1 + actualZDelta, lineColor);
         this.line(x2, y1, z1, x2 - actualXDelta, y1, z1, lineColor);
         this.line(x2, y1, z1, x2, y1 + actualYDelta, z1, lineColor);
         this.line(x2, y1, z1, x2, y1, z1 + actualZDelta, lineColor);
         this.line(x2 - actualXDelta, y1, z1, x2, y1 + actualYDelta, z1, lineColor);
         this.line(x2 - actualXDelta, y1, z1, x2, y1, z1 + actualZDelta, lineColor);
         this.line(x2, y1 + actualYDelta, z1, x2, y1, z1 + actualZDelta, lineColor);
         this.line(x2, y1, z2, x2 - actualXDelta, y1, z2, lineColor);
         this.line(x2, y1, z2, x2, y1 + actualYDelta, z2, lineColor);
         this.line(x2, y1, z2, x2, y1, z2 - actualZDelta, lineColor);
         this.line(x2 - actualXDelta, y1, z2, x2, y1 + actualYDelta, z2, lineColor);
         this.line(x2 - actualXDelta, y1, z2, x2, y1, z2 - actualZDelta, lineColor);
         this.line(x2, y1 + actualYDelta, z2, x2, y1, z2 - actualZDelta, lineColor);
         this.line(x1, y1, z2, x1 + actualXDelta, y1, z2, lineColor);
         this.line(x1, y1, z2, x1, y1 + actualYDelta, z2, lineColor);
         this.line(x1, y1, z2, x1, y1, z2 - actualZDelta, lineColor);
         this.line(x1 + actualXDelta, y1, z2, x1, y1 + actualYDelta, z2, lineColor);
         this.line(x1 + actualXDelta, y1, z2, x1, y1, z2 - actualZDelta, lineColor);
         this.line(x1, y1 + actualYDelta, z2, x1, y1, z2 - actualZDelta, lineColor);
         this.line(x1, y2, z1, x1 + actualXDelta, y2, z1, lineColor);
         this.line(x1, y2, z1, x1, y2 - actualYDelta, z1, lineColor);
         this.line(x1, y2, z1, x1, y2, z1 + actualZDelta, lineColor);
         this.line(x1 + actualXDelta, y2, z1, x1, y2 - actualYDelta, z1, lineColor);
         this.line(x1 + actualXDelta, y2, z1, x1, y2, z1 + actualZDelta, lineColor);
         this.line(x1, y2 - actualYDelta, z1, x1, y2, z1 + actualZDelta, lineColor);
         this.line(x2, y2, z1, x2 - actualXDelta, y2, z1, lineColor);
         this.line(x2, y2, z1, x2, y2 - actualYDelta, z1, lineColor);
         this.line(x2, y2, z1, x2, y2, z1 + actualZDelta, lineColor);
         this.line(x2 - actualXDelta, y2, z1, x2, y2 - actualYDelta, z1, lineColor);
         this.line(x2 - actualXDelta, y2, z1, x2, y2, z1 + actualZDelta, lineColor);
         this.line(x2, y2 - actualYDelta, z1, x2, y2, z1 + actualZDelta, lineColor);
         this.line(x2, y2, z2, x2 - actualXDelta, y2, z2, lineColor);
         this.line(x2, y2, z2, x2, y2 - actualYDelta, z2, lineColor);
         this.line(x2, y2, z2, x2, y2, z2 - actualZDelta, lineColor);
         this.line(x2 - actualXDelta, y2, z2, x2, y2 - actualYDelta, z2, lineColor);
         this.line(x2 - actualXDelta, y2, z2, x2, y2, z2 - actualZDelta, lineColor);
         this.line(x2, y2 - actualYDelta, z2, x2, y2, z2 - actualZDelta, lineColor);
         this.line(x1, y2, z2, x1 + actualXDelta, y2, z2, lineColor);
         this.line(x1, y2, z2, x1, y2 - actualYDelta, z2, lineColor);
         this.line(x1, y2, z2, x1, y2, z2 - actualZDelta, lineColor);
         this.line(x1 + actualXDelta, y2, z2, x1, y2 - actualYDelta, z2, lineColor);
         this.line(x1 + actualXDelta, y2, z2, x1, y2, z2 - actualZDelta, lineColor);
         this.line(x1, y2 - actualYDelta, z2, x1, y2, z2 - actualZDelta, lineColor);
      }
   }

   @Unique
   private void triangle(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, Color color) {
      this.triangle(x1, y1, z1, x2, y2, z2, x3, y3, z3, color, color, color);
   }

   @Unique
   @Override
   public void triangle(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, Color top, Color left, Color right) {
      ((IMesh)this.triangles)
         .triangle(
            this.triangles.vec3(x1, y1, z1).color(top).next(),
            this.triangles.vec3(x2, y2, z2).color(left).next(),
            this.triangles.vec3(x3, y3, z3).color(right).next()
         );
   }

   @Unique
   @Override
   public void octahedron(double x1, double y1, double z1, double x2, double y2, double z2, Color sideColor, Color lineColor, ShapeMode mode) {
      double halfDeltaX = (x2 - x1) * 0.5;
      double halfDeltaY = (y2 - y1) * 0.5;
      double halfDeltaZ = (z2 - z1) * 0.5;
      if (mode.sides()) {
         this.triangle(x1 + halfDeltaX, y1, z1 + halfDeltaZ, x1, y1 + halfDeltaY, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z1, sideColor);
         this.triangle(x1 + halfDeltaX, y1, z1 + halfDeltaZ, x2, y1 + halfDeltaY, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z1, sideColor);
         this.triangle(x1 + halfDeltaX, y1, z1 + halfDeltaZ, x1, y1 + halfDeltaY, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z2, sideColor);
         this.triangle(x1 + halfDeltaX, y1, z1 + halfDeltaZ, x2, y1 + halfDeltaY, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z2, sideColor);
         this.triangle(x1 + halfDeltaX, y2, z1 + halfDeltaZ, x1, y1 + halfDeltaY, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z1, sideColor);
         this.triangle(x1 + halfDeltaX, y2, z1 + halfDeltaZ, x2, y1 + halfDeltaY, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z1, sideColor);
         this.triangle(x1 + halfDeltaX, y2, z1 + halfDeltaZ, x1, y1 + halfDeltaY, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z2, sideColor);
         this.triangle(x1 + halfDeltaX, y2, z1 + halfDeltaZ, x2, y1 + halfDeltaY, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z2, sideColor);
      }

      if (mode.lines()) {
         this.line(x1 + halfDeltaX, y1, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z1, lineColor);
         this.line(x1 + halfDeltaX, y1, z1 + halfDeltaZ, x1, y1 + halfDeltaY, z1 + halfDeltaZ, lineColor);
         this.line(x1 + halfDeltaX, y1, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z2, lineColor);
         this.line(x1 + halfDeltaX, y1, z1 + halfDeltaZ, x2, y1 + halfDeltaY, z1 + halfDeltaZ, lineColor);
         this.line(x1 + halfDeltaX, y2, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z1, lineColor);
         this.line(x1 + halfDeltaX, y2, z1 + halfDeltaZ, x1, y1 + halfDeltaY, z1 + halfDeltaZ, lineColor);
         this.line(x1 + halfDeltaX, y2, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z2, lineColor);
         this.line(x1 + halfDeltaX, y2, z1 + halfDeltaZ, x2, y1 + halfDeltaY, z1 + halfDeltaZ, lineColor);
         this.line(x1, y1 + halfDeltaY, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z1, lineColor);
         this.line(x1 + halfDeltaX, y1 + halfDeltaY, z1, x2, y1 + halfDeltaY, z1 + halfDeltaZ, lineColor);
         this.line(x2, y1 + halfDeltaY, z1 + halfDeltaZ, x1 + halfDeltaX, y1 + halfDeltaY, z2, lineColor);
         this.line(x1 + halfDeltaX, y1 + halfDeltaY, z2, x1, y1 + halfDeltaY, z1 + halfDeltaZ, lineColor);
      }
   }
}
