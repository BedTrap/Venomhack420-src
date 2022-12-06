package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import venomhack.modules.movement.Moses;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Redirect(
      method = {"updateSwimming"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/Entity;isSprinting()Z"
)
   )
   private boolean updateSwimmingisSprintingProxy(Entity self) {
      return self.isSprinting() && Modules.get().isActive(Moses.class) ? false : self.isSprinting();
   }
}
