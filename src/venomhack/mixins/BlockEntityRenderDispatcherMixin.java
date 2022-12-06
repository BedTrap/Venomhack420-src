package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.render.BetterChams;

@Mixin({BlockEntityRenderDispatcher.class})
public class BlockEntityRenderDispatcherMixin {
   @Inject(
      method = {"render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"},
      at = {@At("HEAD")}
   )
   private <E extends BlockEntity> void render(E blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      BetterChams BetterChams = (BetterChams)Modules.get().get(BetterChams.class);
      if (BetterChams.isActive()) {
         GL11.glDisable(32823);
         GL11.glDepthRange(0.0, 1.0);
      }
   }
}
