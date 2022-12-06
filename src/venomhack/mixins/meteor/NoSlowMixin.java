package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import venomhack.utils.UtilsPlus;

@Mixin(
   value = {NoSlow.class},
   remap = false
)
public class NoSlowMixin {
   @Redirect(
      method = {"onPreTick"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"
),
      remap = true
   )
   private Block isCobbed(BlockState instance) {
      for(BlockPos pos : UtilsPlus.playerBlocks(MeteorClient.mc.player)) {
         if (MeteorClient.mc.world.getBlockState(pos).isOf(Blocks.COBWEB)) {
            return Blocks.COBWEB;
         }
      }

      return Blocks.AIR;
   }
}
