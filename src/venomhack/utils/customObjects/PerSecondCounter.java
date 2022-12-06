package venomhack.utils.customObjects;

import java.util.function.Predicate;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class PerSecondCounter {
   private final int[] counts = new int[20];

   public void count(Item item) {
      for(int i = this.counts.length - 1; i >= 0; --i) {
         if (i == 0) {
            this.counts[0] = count((Predicate<Item>)(item1 -> item1 == item));
         } else {
            this.counts[i] = this.counts[i - 1];
         }
      }
   }

   public int get() {
      return this.counts[19] - this.counts[0];
   }

   public static int count(Predicate<Item> predicate) {
      int count = 0;

      for(int i = 0; i < 36; ++i) {
         ItemStack stack = MeteorClient.mc.player.getInventory().getStack(i);
         if (predicate.test(stack.getItem())) {
            count += stack.getCount();
         }
      }

      ItemStack offhandStack = MeteorClient.mc.player.getOffHandStack();
      if (predicate.test(offhandStack.getItem())) {
         count += offhandStack.getCount();
      }

      ItemStack cursorStack = MeteorClient.mc.player.currentScreenHandler.getCursorStack();
      if (predicate.test(cursorStack.getItem())) {
         count += cursorStack.getCount();
      }

      for(int i = 0; i < 4; ++i) {
         ItemStack stack = MeteorClient.mc.player.playerScreenHandler.getCraftingInput().getStack(i);
         if (predicate.test(stack.getItem())) {
            count += stack.getCount();
         }
      }

      return count;
   }
}
