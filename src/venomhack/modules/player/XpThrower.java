package venomhack.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class XpThrower extends ModuleHelper {
   private final Setting<Boolean> rotate = this.setting("rotate", "Whether to rotate while throwing xp or not", Boolean.valueOf(true));
   private final Setting<Integer> packetsPerTick = this.setting("bottles-per-tick", "How many bottles to throw per tick", Integer.valueOf(1), 1.0, 10.0);
   private FindItemResult result;

   public XpThrower() {
      super(Venomhack420.CATEGORY, "xp-thrower", "Repairs items automatically using EXP bottles.");
   }

   @EventHandler
   private void postTick(Pre event) {
      this.result = InvUtils.findInHotbar(new Item[]{Items.EXPERIENCE_BOTTLE});
      if (!this.result.found()) {
         this.toggleWithError(273462397, "Couldn't find XP bottles in your hotbar... Disabling!", new Object[0]);
      } else {
         int fullPieces = 0;
         int mendablePieces = 0;

         for(int i = 0; i < 4; ++i) {
            ItemStack itemStack = this.mc.player.getInventory().getArmorStack(i);
            if (itemStack != null) {
               if (EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack) > 0) {
                  ++mendablePieces;
               }

               if (!itemStack.isDamaged()) {
                  ++fullPieces;
               }
            }
         }

         if (mendablePieces == 0) {
            this.toggleWithError(273462397, "Couldn't find a piece of armour with mending... Disabling!", new Object[0]);
         } else if (fullPieces >= mendablePieces) {
            this.toggleWithInfo(273462397, "Your armour is fully repaired... Disabling!", new Object[0]);
         } else {
            if (this.rotate.get()) {
               Rotations.rotate((double)this.mc.player.getYaw(), 90.0, 15, this::throwXp);
            } else {
               this.throwXp();
            }
         }
      }
   }

   private void throwXp() {
      InvUtils.swap(this.result.slot(), true);

      for(int i = 0; i < this.packetsPerTick.get(); ++i) {
         this.mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(this.result.getHand(), 0));
      }

      this.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(this.result.getHand()));
      InvUtils.swapBack();
   }
}
