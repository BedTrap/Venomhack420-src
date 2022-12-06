package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.systems.modules.player.MiddleClickExtra;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {MiddleClickExtra.class},
   remap = false
)
public class MiddleClickExtraMixin {
   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void onMiddleClick(MouseButtonEvent event, CallbackInfo ci) {
      if (MeteorClient.mc.currentScreen != null) {
         ci.cancel();
      }
   }
}
