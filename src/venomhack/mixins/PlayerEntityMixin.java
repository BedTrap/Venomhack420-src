package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.movement.Anchor;

@Mixin({PlayerEntity.class})
public class PlayerEntityMixin {
   @Inject(
      method = {"jump"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void dontJump(CallbackInfo info) {
      Anchor module = (Anchor)Modules.get().get(Anchor.class);
      if (module.isActive() && module.cancelJump) {
         info.cancel();
      }
   }
}
