package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.Quaternion;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation.class_811;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.render.DroppedItemsView;

@Mixin({ItemRenderer.class})
public class ItemRendererMixin {
   @Inject(
      method = {"renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V"
)}
   )
   private void renderItem(
      ItemStack stack,
      class_811 renderMode,
      boolean leftHanded,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      int overlay,
      BakedModel model,
      CallbackInfo ci
   ) {
      DroppedItemsView dropped = (DroppedItemsView)Modules.get().get(DroppedItemsView.class);
      if (renderMode == class_811.GROUND && dropped.isActive()) {
         if (stack.getItem() instanceof BlockItem) {
            matrices.multiply(
               new Quaternion(
                  ((Double)dropped.rotationXBlocksDropped.get()).floatValue(),
                  ((Double)dropped.rotationYBlocksDropped.get()).floatValue(),
                  ((Double)dropped.rotationZBlocksDropped.get()).floatValue(),
                  true
               )
            );
            matrices.scale(
               ((Double)dropped.scaleXYZBlocksDropped.get()).floatValue(),
               ((Double)dropped.scaleXYZBlocksDropped.get()).floatValue(),
               ((Double)dropped.scaleXYZBlocksDropped.get()).floatValue()
            );
         } else {
            matrices.multiply(
               new Quaternion(
                  ((Double)dropped.rotationXDropped.get()).floatValue(),
                  ((Double)dropped.rotationYDropped.get()).floatValue(),
                  ((Double)dropped.rotationZDropped.get()).floatValue(),
                  true
               )
            );
            matrices.scale(
               ((Double)dropped.scaleXDropped.get()).floatValue(),
               ((Double)dropped.scaleYDropped.get()).floatValue(),
               ((Double)dropped.scaleZDropped.get()).floatValue()
            );
         }
      }
   }
}
