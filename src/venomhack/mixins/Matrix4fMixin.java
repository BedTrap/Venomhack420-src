package venomhack.mixins;

import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import venomhack.mixinInterface.IMatrix4f;

@Mixin({Matrix4f.class})
public class Matrix4fMixin implements IMatrix4f {
   @Shadow
   protected float a00;
   @Shadow
   protected float a01;
   @Shadow
   protected float a02;
   @Shadow
   protected float a03;
   @Shadow
   protected float a10;
   @Shadow
   protected float a11;
   @Shadow
   protected float a12;
   @Shadow
   protected float a13;
   @Shadow
   protected float a20;
   @Shadow
   protected float a21;
   @Shadow
   protected float a22;
   @Shadow
   protected float a23;
   @Shadow
   protected float a30;
   @Shadow
   protected float a31;
   @Shadow
   protected float a32;
   @Shadow
   protected float a33;

   @Unique
   @Override
   public void loadFromArray(float[] arr) {
      this.a00 = arr[0];
      this.a01 = arr[1];
      this.a02 = arr[2];
      this.a03 = arr[3];
      this.a10 = arr[4];
      this.a11 = arr[5];
      this.a12 = arr[6];
      this.a13 = arr[7];
      this.a20 = arr[8];
      this.a21 = arr[9];
      this.a22 = arr[10];
      this.a23 = arr[11];
      this.a30 = arr[12];
      this.a31 = arr[13];
      this.a32 = arr[14];
      this.a33 = arr[15];
   }
}
