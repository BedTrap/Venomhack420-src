package venomhack.modules.misc;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.List;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.block.Block;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.block.Material;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;
import venomhack.utils.RandUtils;

public class AutoCrafter extends ModuleHelper {
   private final SettingGroup sgBeds = this.group("Bed Restock");
   private final Setting<Integer> delay = this.setting("delay", "How many ticks to wait in between crafting.", Integer.valueOf(2));
   private final Setting<Boolean> silent = this.setting("silent", "WARNING BUGGY! Will craft without opening the gui.", Boolean.valueOf(false));
   private final Setting<Boolean> close = this.setting(
      "close-on-finished", "Closes the crafting table gui when done.", Boolean.valueOf(true), () -> !this.silent.get()
   );
   private final Setting<Boolean> openBook = this.setting(
      "open-recipe-book", "Automatically opens the recipe book when its not open yet.", Boolean.valueOf(false)
   );
   private final Setting<List<Item>> items = this.setting(
      "items", "Which items to craft.", this.sgGeneral, false, item -> true, null, null, null, new Item[]{Items.PURPLE_BED}
   );
   private final Setting<Boolean> fastUnstackables = this.setting(
      "fast-unstackable-crafting",
      "Speeds up crafting of unstackable items.",
      Boolean.valueOf(true),
      () -> ((List)this.items.get()).stream().anyMatch(item -> !item.getDefaultStack().isStackable())
   );
   public final Setting<Integer> maxBeds = this.setting(
      "max-bed-amount",
      "How many beds to get max via crafting.",
      Integer.valueOf(36),
      this.sgBeds,
      () -> ((List)this.items.get()).stream().anyMatch(item -> item instanceof BedItem),
      0.0,
      36.0
   );
   private int delayLeft;
   private boolean didAnything;
   private CraftingScreen screen;

   public AutoCrafter() {
      super(Venomhack420.CATEGORY, "auto-crafter", "Automatically crafts items.");
   }

   @EventHandler
   private void onTick(Pre event) {
      CraftingScreen currentScreen = null;
      Screen var4 = this.mc.currentScreen;
      if (var4 instanceof CraftingScreen c) {
         currentScreen = c;
      } else if (this.silent.get()) {
         currentScreen = this.screen;
      }

      if (currentScreen != null) {
         if (this.delayLeft <= 0) {
            ScreenHandler handler = currentScreen.getScreenHandler();

            for(RecipeResultCollection recipeResultCollection : this.mc.player.getRecipeBook().getOrderedResults()) {
               for(Recipe<?> recipe : recipeResultCollection.getRecipes(true)) {
                  Item item = recipe.getOutput().getItem();
                  if (((List)this.items.get()).contains(item)) {
                     boolean isBed = recipe.getOutput().getItem() instanceof BedItem;
                     FindItemResult beds = null;
                     if (isBed) {
                        beds = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
                     }

                     int emptySlots = RandUtils.countEmptySlots();
                     if (emptySlots > 0 && (!isBed || beds.count() < this.maxBeds.get())) {
                        if (this.fastUnstackables.get() && !item.getDefaultStack().isStackable()) {
                           if (this.delayLeft == this.delay.get()) {
                              --this.delayLeft;
                              return;
                           }

                           for(int i = 0; i < (isBed ? Math.min(this.maxBeds.get() - beds.count(), emptySlots) : emptySlots); ++i) {
                              this.mc.interactionManager.clickRecipe(handler.syncId, recipe, false);
                           }
                        } else {
                           this.mc.interactionManager.clickRecipe(handler.syncId, recipe, item.getDefaultStack().isStackable());
                        }

                        this.mc.interactionManager.clickSlot(handler.syncId, 0, 1, SlotActionType.QUICK_MOVE, this.mc.player);
                        this.mc.player.getInventory().updateItems();
                        this.didAnything = true;
                        this.delayLeft = this.delay.get();
                        break;
                     }

                     if (this.didAnything || this.silent.get() && this.mc.currentScreen == null) {
                        this.close(handler);
                     }
                  }
               }
            }

            if (this.didAnything && this.delayLeft <= 0 && this.noBedMaterials()) {
               this.close(handler);
            }
         } else {
            --this.delayLeft;
         }
      }
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      Screen var3 = event.screen;
      if (var3 instanceof CraftingScreen c) {
         this.screen = c;
         if (this.openBook.get() && !c.getRecipeBookWidget().isOpen()) {
            c.getRecipeBookWidget().toggleOpen();
         }

         if (this.silent.get()) {
            event.cancel();
         }
      }
   }

   public boolean noBedMaterials() {
      Int2IntMap colours = new Int2IntOpenHashMap();
      short planks = 0;

      for(int i = 0; i < 36; ++i) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         Item item = stack.getItem();
         Block block = Block.getBlockFromItem(item);
         if (block.getDefaultState().getMaterial().equals(Material.WOOL)) {
            int color = block.getDefaultMapColor().id;

            for(Item item2 : (List)this.items.get()) {
               if (item2 instanceof BedItem && Block.getBlockFromItem(item2).getDefaultMapColor().id == color) {
                  if (planks > 2 && stack.getCount() > 2) {
                     return false;
                  }

                  colours.put(color, colours.getOrDefault(color, 0) + stack.getCount());
                  break;
               }
            }
         } else if (planks < 3
            && (stack.getItem().getName().getString().contains("Planks") || stack.getItem().getName().getString().contains("bretter"))) {
            planks = (short)(planks + stack.getCount());
         }
      }

      if (planks > 2) {
         IntIterator var10 = colours.values().iterator();

         while(var10.hasNext()) {
            int woolCount = var10.next();
            if (woolCount > 2) {
               return false;
            }
         }
      }

      return true;
   }

   private void close(ScreenHandler handler) {
      if (this.silent.get()) {
         this.mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(handler.syncId));
      } else {
         if (!this.close.get()) {
            return;
         }

         this.mc.player.closeHandledScreen();
      }

      this.screen = null;
      this.didAnything = false;
      this.mc.player.getInventory().updateItems();
   }

   public void onActivate() {
      this.delayLeft = 0;
      this.screen = null;
      this.didAnything = false;
   }
}
