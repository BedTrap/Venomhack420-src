package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.renderer.Mesh;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import venomhack.mixinInterface.IMesh;

@Mixin(
   value = {Mesh.class},
   remap = false
)
public abstract class MeshMixin implements IMesh {
   @Shadow(
      remap = false
   )
   public abstract void growIfNeeded();

   @Unique
   @Override
   public void triangle(int i1, int i2, int i3) {
      long p = ((MeshAccessor)this).indicesPointer() + (long)((MeshAccessor)this).indicesCount() * 4L;
      MemoryUtil.memPutInt(p, i1);
      MemoryUtil.memPutInt(p + 4L, i2);
      MemoryUtil.memPutInt(p + 8L, i3);
      ((MeshAccessor)this).setIndicesCount(((MeshAccessor)this).indicesCount() + 3);
      this.growIfNeeded();
   }
}
