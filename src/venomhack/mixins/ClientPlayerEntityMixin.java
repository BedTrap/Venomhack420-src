package venomhack.mixins;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerEntity.class})
public class ClientPlayerEntityMixin {
   @Inject(
      method = {"getPermissionLevel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getPerms(CallbackInfoReturnable<Integer> cir) {
      cir.setReturnValue(2);
   }
}
