package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.Immediate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.render.BetterPops;

@Mixin({ParticleManager.class})
public class ParticleManagerMixin {
   @Inject(
      method = {"renderParticles"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void balls(MatrixStack matrices, class_4598 immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float f, CallbackInfo ci) {
      BetterPops betterPops = (BetterPops)Modules.get().get(BetterPops.class);
      if (betterPops.isActive()) {
      }
   }
}
