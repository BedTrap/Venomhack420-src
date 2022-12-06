package venomhack.enums;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.item.Items;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;

public enum SurroundBlocks {
   OBSIDIAN("Obsidian"),
   ENDER_CHEST("Ender Chest"),
   CRYING_OBSIDIAN("Crying Obsidian"),
   NETHERITE_BLOCK("Netherite Block"),
   ANCIENT_DEBRIS("Ancient Debris"),
   RESPAWN_ANCHOR("Respawn Anchor"),
   ANVIL("Anvil"),
   HELD("Held Item");

   private final String title;

   public FindItemResult getBlock() {
      return InvUtils.findInHotbar(stack -> {
         return switch(this) {
            case OBSIDIAN -> Items.OBSIDIAN == stack.getItem();
            case ANCIENT_DEBRIS -> Items.ANCIENT_DEBRIS == stack.getItem();
            case CRYING_OBSIDIAN -> Items.CRYING_OBSIDIAN == stack.getItem();
            case NETHERITE_BLOCK -> Items.NETHERITE_BLOCK == stack.getItem();
            case ENDER_CHEST -> Items.ENDER_CHEST == stack.getItem();
            case RESPAWN_ANCHOR -> Items.RESPAWN_ANCHOR == stack.getItem() && PlayerUtils.getDimension() == Dimension.Nether;
            case ANVIL -> Block.getBlockFromItem(stack.getItem()) instanceof AnvilBlock;
            case HELD -> !Block.getBlockFromItem(stack.getItem()).equals(Blocks.AIR);
         };
      });
   }

   private SurroundBlocks(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }
}
