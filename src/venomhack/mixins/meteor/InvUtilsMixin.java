package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {InvUtils.class},
   remap = false
)
public class InvUtilsMixin {
   @Redirect(
      method = {"findFastestTool"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/item/ItemStack;getMiningSpeedMultiplier(Lnet/minecraft/block/BlockState;)F"
),
      remap = true
   )
   private static float findFastest(ItemStack itemStack, BlockState state) {
      return !itemStack.isSuitableFor(state) ? -1.0F : itemStack.getMiningSpeedMultiplier(state);
   }
}
