package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.block.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import venomhack.mixinInterface.IBlockItem;
import venomhack.modules.misc.PacketPlace;

@Mixin({BlockItem.class})
public abstract class BlockItemMixin implements IBlockItem {
   @Shadow
   protected abstract boolean canPlace(ItemPlacementContext var1, BlockState var2);

   @Shadow
   @Nullable
   protected abstract BlockState getPlacementState(ItemPlacementContext var1);

   @Override
   public boolean canBePlaced(ItemPlacementContext context, BlockState state) {
      return this.canPlace(context, state);
   }

   @Override
   public BlockState getThePlacementState(ItemPlacementContext context) {
      return this.getPlacementState(context);
   }

   @Inject(
      method = {"place(Lnet/minecraft/item/ItemPlacementContext;Lnet/minecraft/block/BlockState;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPlace(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
      if (Modules.get().isActive(PacketPlace.class)) {
         cir.cancel();
      }
   }
}
