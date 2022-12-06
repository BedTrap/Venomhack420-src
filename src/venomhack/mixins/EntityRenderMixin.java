package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import venomhack.modules.combat.OneShot;

@Mixin({EntityRenderer.class})
public class EntityRenderMixin<T extends Entity> {
   @Inject(
      method = {"shouldRender"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void noArrowRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      OneShot sniper = (OneShot)Modules.get().get(OneShot.class);
      if (entity instanceof ArrowEntity && sniper.isActive() && sniper.noRender.get()) {
         cir.setReturnValue(false);
      }
   }
}
