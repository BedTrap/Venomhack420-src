package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Sent;
import meteordevelopment.meteorclient.systems.modules.player.NoInteract;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.class_2849;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import venomhack.mixinInterface.INoInteract;
import venomhack.utils.PlayerUtils2;

@Mixin(
   value = {NoInteract.class},
   remap = false
)
public abstract class NoInteractMixin implements INoInteract {
   @Unique
   private BlockHitResult result = null;

   @Shadow
   protected abstract boolean shouldInteractBlock(BlockHitResult var1, Hand var2);

   @Overwrite(
      remap = false
   )
   @EventHandler
   private void onInteractBlock(InteractBlockEvent event) {
      if (!this.shouldInteractBlock(event.result, event.hand)) {
         ItemStack stack = MeteorClient.mc.player.getStackInHand(event.hand);
         Item var4 = stack.getItem();
         if (!(var4 instanceof BlockItem blockItem)
            || !PlayerUtils2.canPlace(new ItemPlacementContext(MeteorClient.mc.player, event.hand, stack, event.result), blockItem)) {
            event.cancel();
            return;
         }

         if (!MeteorClient.mc.player.isSneaking()) {
            MeteorClient.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(MeteorClient.mc.player, class_2849.PRESS_SHIFT_KEY));
            this.result = event.result;
         }
      }
   }

   @Unique
   @Override
   public boolean shouldInteract(BlockHitResult hitResult, Hand hand) {
      return this.shouldInteractBlock(hitResult, hand);
   }

   @Unique
   @EventHandler
   private void onPacketSent(Sent event) {
      Packet var3 = event.packet;
      if (var3 instanceof PlayerInteractBlockC2SPacket packet && this.result != null && packet.getBlockHitResult().equals(this.result)) {
         MeteorClient.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(MeteorClient.mc.player, class_2849.RELEASE_SHIFT_KEY));
         this.result = null;
      }
   }
}
