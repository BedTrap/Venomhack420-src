package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.AirPlace;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Hand;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.class_240;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.utils.BlockUtils2;
import venomhack.utils.PlayerUtils2;
import venomhack.utils.RenderUtils2;

@Mixin(
   value = {AirPlace.class},
   remap = false
)
public class AirPlaceMixin {
   @Shadow
   private HitResult hitResult;
   @Unique
   private Hand hand;

   @Inject(
      method = {"onTick"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   private void onTick(CallbackInfo ci) {
      if (MeteorClient.mc.crosshairTarget.getType() != class_240.MISS) {
         ((AirPlaceAccessor)Modules.get().get(AirPlace.class)).setHitResult(null);
         ci.cancel();
      }
   }

   @Redirect(
      method = {"onTick"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;",
   remap = true
)
   )
   private Item offhandSupport(ItemStack stack) {
      Item var3 = MeteorClient.mc.player.getMainHandStack().getItem();
      if (var3 instanceof BlockItem item) {
         this.hand = Hand.MAIN_HAND;
         return item;
      } else {
         var3 = MeteorClient.mc.player.getOffHandStack().getItem();
         if (var3 instanceof BlockItem item
            && !MeteorClient.mc.player.isUsingItem()
            && !MeteorClient.mc.player.getMainHandStack().getItem().isFood()) {
            this.hand = Hand.OFF_HAND;
            return item;
         }

         this.hand = null;
         return Items.AIR;
      }
   }

   @Redirect(
      method = {"onTick"},
      at = @At(
   value = "INVOKE",
   target = "Lmeteordevelopment/meteorclient/utils/world/BlockUtils;place(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Hand;IZIZZZ)Z",
   remap = true
)
   )
   private boolean onTickRedirect(
      BlockPos pos, Hand wrong, int slot, boolean rotate, int rotationPrio, boolean swing, boolean checkEntities, boolean swapBack
   ) {
      if (this.hand == null) {
         return false;
      } else {
         ItemStack stack = MeteorClient.mc.player.getStackInHand(this.hand);
         Item context = stack.getItem();
         if (context instanceof BlockItem blockItem) {
            ItemPlacementContext contextx = new ItemPlacementContext(MeteorClient.mc.player, this.hand, stack, (BlockHitResult)this.hitResult);
            if (PlayerUtils2.canPlace(contextx, blockItem) && !BlockUtils2.invalidPos(contextx.getBlockPos())) {
               FindItemResult result = new FindItemResult(this.hand == Hand.OFF_HAND ? 45 : MeteorClient.mc.player.getInventory().selectedSlot, 1);
               return BlockUtils2.placeBlock(result, pos, false, 0, true, false, swing, false);
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   @Redirect(
      method = {"onRender"},
      at = @At(
   value = "INVOKE",
   target = "Lmeteordevelopment/meteorclient/renderer/Renderer3D;box(Lnet/minecraft/util/math/BlockPos;Lmeteordevelopment/meteorclient/utils/render/color/Color;Lmeteordevelopment/meteorclient/utils/render/color/Color;Lmeteordevelopment/meteorclient/renderer/ShapeMode;I)V",
   remap = true
)
   )
   private void onRender(Renderer3D renderer, BlockPos pos, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
      if (this.hand != null) {
         ItemStack stack = MeteorClient.mc.player.getStackInHand(this.hand);
         Item context = stack.getItem();
         if (context instanceof BlockItem blockItem) {
            ItemPlacementContext contextx = new ItemPlacementContext(MeteorClient.mc.player, this.hand, stack, (BlockHitResult)this.hitResult);
            if (PlayerUtils2.canPlace(contextx, blockItem) && !BlockUtils2.invalidPos(contextx.getBlockPos())) {
               if (blockItem instanceof BedItem) {
                  RenderUtils2.renderBed(renderer, sideColor, lineColor, mode, false, 1, 1, 0, MeteorClient.mc.player.getHorizontalFacing(), pos);
               } else {
                  RenderUtils2.renderState(renderer, Block.getBlockFromItem(blockItem).getDefaultState(), pos, mode, sideColor, lineColor);
               }
            }
         }
      }
   }

   @Redirect(
      method = {"onRender"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;",
   remap = true
)
   )
   private Item offhandRender(ItemStack stack) {
      Item var4 = MeteorClient.mc.player.getMainHandStack().getItem();
      if (var4 instanceof BlockItem) {
         return (BlockItem)var4;
      } else {
         var4 = MeteorClient.mc.player.getOffHandStack().getItem();
         if (var4 instanceof BlockItem offItem
            && !MeteorClient.mc.player.isUsingItem()
            && !MeteorClient.mc.player.getMainHandStack().getItem().isFood()) {
            return offItem;
         }

         return Items.AIR;
      }
   }

   @Inject(
      method = {"onRender"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   private void onRenderCancel(Render3DEvent event, CallbackInfo ci) {
      if (MeteorClient.mc.player.isSpectator()) {
         ci.cancel();
      }
   }
}
