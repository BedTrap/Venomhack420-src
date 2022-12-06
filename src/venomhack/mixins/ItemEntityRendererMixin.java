package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.math.Quaternion;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.render.DroppedItemsView;

@Mixin({ItemEntityRenderer.class})
public abstract class ItemEntityRendererMixin {
   @Inject(
      method = {"render*"},
      at = {@At("HEAD")}
   )
   private void render(ItemEntity itemEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      DroppedItemsView dropped = (DroppedItemsView)Modules.get().get(DroppedItemsView.class);
      if (dropped.isActive() && !itemEntity.getStack().isEmpty()) {
         if (itemEntity.getStack().getItem() instanceof BlockItem) {
            matrixStack.multiply(
               new Quaternion(
                  ((Double)dropped.rotationXBlocksDropped.get()).floatValue(),
                  ((Double)dropped.rotationYBlocksDropped.get()).floatValue(),
                  ((Double)dropped.rotationZBlocksDropped.get()).floatValue(),
                  true
               )
            );
            matrixStack.scale(
               ((Double)dropped.scaleXYZBlocksDropped.get()).floatValue(),
               ((Double)dropped.scaleXYZBlocksDropped.get()).floatValue(),
               ((Double)dropped.scaleXYZBlocksDropped.get()).floatValue()
            );
         } else {
            matrixStack.multiply(
               new Quaternion(
                  ((Double)dropped.rotationXDropped.get()).floatValue(),
                  ((Double)dropped.rotationYDropped.get()).floatValue(),
                  ((Double)dropped.rotationZDropped.get()).floatValue(),
                  true
               )
            );
            matrixStack.scale(
               ((Double)dropped.scaleXDropped.get()).floatValue(),
               ((Double)dropped.scaleYDropped.get()).floatValue(),
               ((Double)dropped.scaleZDropped.get()).floatValue()
            );
         }
      }
   }
}
