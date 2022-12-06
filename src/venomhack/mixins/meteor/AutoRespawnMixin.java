package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.systems.modules.misc.AutoRespawn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.Venomhack420;

@Mixin(
   value = {AutoRespawn.class},
   remap = false
)
public class AutoRespawnMixin {
   @Inject(
      method = {"onOpenScreenEvent"},
      at = {@At("TAIL")},
      remap = false
   )
   private void onDeathScreen(OpenScreenEvent event, CallbackInfo ci) {
      Venomhack420.STATS.setDead();
   }
}
