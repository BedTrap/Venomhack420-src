package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.renderer.Mesh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {Mesh.class},
   remap = false
)
public interface MeshAccessor {
   @Accessor(
      value = "indicesPointer",
      remap = false
   )
   long indicesPointer();

   @Accessor(
      value = "indicesCount",
      remap = false
   )
   int indicesCount();

   @Accessor(
      value = "indicesCount",
      remap = false
   )
   void setIndicesCount(int var1);
}
