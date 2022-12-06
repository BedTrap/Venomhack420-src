package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.render.Tracers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Tracers.class},
   remap = false
)
public class TracersMixin {
   @Shadow(
      remap = false
   )
   @Final
   private SettingGroup sgGeneral;
   @Unique
   private Setting<Boolean> friends = null;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")},
      remap = false
   )
   private void onInit(CallbackInfo ci) {
      this.friends = this.sgGeneral
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("show-friends")).description("Whether to draw tracers to friends or not.")).defaultValue(false))
               .build()
         );
   }

   @Redirect(
      method = {"onRender"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/network/ClientPlayerEntity;distanceTo(Lnet/minecraft/entity/Entity;)F",
   remap = true
)
   )
   private float modifyDistanceTo(ClientPlayerEntity instance, Entity entity) {
      return !this.friends.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity)entity)
         ? Float.MAX_VALUE
         : instance.distanceTo(entity);
   }
}
