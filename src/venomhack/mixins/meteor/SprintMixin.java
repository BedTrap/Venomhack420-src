package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.movement.Sprint;
import meteordevelopment.orbit.EventHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Sprint.class},
   remap = false
)
public class SprintMixin {
   @Shadow(
      remap = false
   )
   @Final
   private SettingGroup sgGeneral;
   @Shadow(
      remap = false
   )
   @Final
   private Setting<Boolean> whenStationary;
   @Unique
   private Setting<Boolean> multi;
   @Unique
   private Setting<Boolean> inWater;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")},
      remap = false
   )
   private void onInit(CallbackInfo ci) {
      this.multi = this.sgGeneral
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("multi-directional")).description("Sprints in all directions, not just forward."))
                  .defaultValue(true))
               .build()
         );
      this.inWater = this.sgGeneral
         .add(((Builder)((Builder)((Builder)new Builder().name("in-water")).description("Sprints in water.")).defaultValue(false)).build());
   }

   @EventHandler
   @Overwrite(
      remap = false
   )
   private void onTick(Post event) {
      if (MeteorClient.mc.player.getHungerManager().getFoodLevel() > 6) {
         if (this.inWater.get() || !MeteorClient.mc.player.isSubmergedInWater()) {
            if (this.whenStationary.get()) {
               MeteorClient.mc.player.setSprinting(true);
            } else if (MeteorClient.mc.player.forwardSpeed != 0.0F || this.multi.get() && MeteorClient.mc.player.sidewaysSpeed != 0.0F) {
               MeteorClient.mc.player.setSprinting(true);
            }
         }
      }
   }
}
