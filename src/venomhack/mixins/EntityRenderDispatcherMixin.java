package venomhack.mixins;

import com.mojang.blaze3d.platform.GlStateManager;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.render.BetterChams;

@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private <E extends Entity> void render(
      E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci
   ) {
      BetterChams BetterChams = (BetterChams)Modules.get().get(BetterChams.class);
      if (BetterChams.isActive()) {
         GlStateManager._depthMask(true);
         GL11.glEnable(32823);
         GL11.glDepthRange(0.0, 0.01);
      }
   }
}
