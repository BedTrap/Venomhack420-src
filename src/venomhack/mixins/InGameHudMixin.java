package venomhack.mixins;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.combat.OneShot;

@Mixin({InGameHud.class})
public class InGameHudMixin extends DrawableHelper {
   @Shadow
   private int scaledWidth;
   @Shadow
   private int scaledHeight;

   @Inject(
      method = {"renderCrosshair"},
      at = {@At("RETURN")}
   )
   private void drawHit(MatrixStack matrices, CallbackInfo ci) {
      OneShot sniper = (OneShot)Modules.get().get(OneShot.class);
      if (sniper.isActive() && sniper.hitMarksCrosshair.get() && sniper.visibleTicks > 0) {
         RenderSystem.setShaderTexture(0, new Identifier("venomhack", "hit.png"));
         drawTexture(matrices, (this.scaledWidth - 11) / 2, (this.scaledHeight - 11) / 2, 0.0F, 0.0F, 11, 11, 11, 11);
      }
   }
}
