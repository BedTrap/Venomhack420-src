package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import venomhack.modules.render.BetterChams;

@Mixin({Chams.class})
public class ChamsMixin {
   @Inject(
      method = {"shouldRender"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void shouldRender(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (((BetterChams)Modules.get().get(BetterChams.class)).isActive()) {
         cir.setReturnValue(false);
      }
   }
}
