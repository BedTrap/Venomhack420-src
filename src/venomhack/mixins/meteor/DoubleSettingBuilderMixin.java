package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting.Builder;
import meteordevelopment.meteorclient.settings.Setting.SettingBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {Builder.class},
   remap = false
)
public abstract class DoubleSettingBuilderMixin extends SettingBuilder<Builder, Double, DoubleSetting> {
   protected DoubleSettingBuilderMixin(double defaultValue) {
      super(defaultValue);
   }

   @Inject(
      method = {"build()Lmeteordevelopment/meteorclient/settings/DoubleSetting;"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   public void dwa(CallbackInfoReturnable<DoubleSetting> cir) {
      if (this.name.equals("horizontal-speed") && this.description.equals("How fast you go forward and backward.")) {
         cir.setReturnValue(
            ((Builder)((Builder)new Builder().name("horizontal-speed")).description("How fast you go forward and backward in meters per second."))
               .defaultValue(45.0)
               .sliderMax(50.0)
               .build()
         );
      } else if (this.name.equals("vertical-speed") && this.description.equals("How fast you go up and down.")) {
         cir.setReturnValue(
            ((Builder)((Builder)new Builder().name("vertical-speed")).description("How fast you go up and down in meters per second."))
               .defaultValue(16.0)
               .sliderMax(50.0)
               .build()
         );
      } else if (this.name.equals("fall-multiplier")
         && this.description.equals("Controls how fast will you go down naturally.")
         && ((Double)this.defaultValue).equals(0.01)) {
         cir.setReturnValue(
            ((Builder)((Builder)new Builder().name("fall-multiplier")).description("Controls how fast you will go down naturally."))
               .defaultValue(0.0)
               .sliderMax(2.0)
               .build()
         );
      }
   }
}
