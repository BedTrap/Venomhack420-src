package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.settings.Setting.SettingBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {Builder.class},
   remap = false
)
public abstract class BoolSettingBuilderMixin extends SettingBuilder<Builder, Boolean, BoolSetting> {
   protected BoolSettingBuilderMixin(boolean defaultValue) {
      super(defaultValue);
   }

   @Inject(
      method = {"build()Lmeteordevelopment/meteorclient/settings/BoolSetting;"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   public void dwa(CallbackInfoReturnable<BoolSetting> cir) {
      if (this.name.equals("stop-in-water") && this.description.equals("Stops flying in water.")) {
         cir.setReturnValue(
            ((Builder)((Builder)((Builder)new Builder().name("stop-in-water")).description("Stop while flying in water.")).defaultValue(false)).build()
         );
      } else if (this.name.equals("insta-drop")
         && this.description.equals("Makes you drop out of flight instantly.")
         && ((Boolean)this.defaultValue).equals(false)) {
         cir.setReturnValue(
            ((Builder)((Builder)((Builder)new Builder().name("insta-drop")).description("Makes you drop out of flight instantly.")).defaultValue(true))
               .build()
         );
      }
   }
}
