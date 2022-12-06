package venomhack.modules.world.villager_trader;

import java.util.ArrayList;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;

public class BetterGuiMerchant extends MerchantScreen {
   private final ArrayList<Item> wantedTradeItems;

   public BetterGuiMerchant(MerchantScreenHandler handler, PlayerInventory inv, Text title, ArrayList<Item> wantedTrades) {
      super(handler, inv, title);
      this.wantedTradeItems = wantedTrades;
   }

   public int getWantedItemIndex(Item item) {
      TradeOfferList trades = ((MerchantScreenHandler)this.handler).getRecipes();

      for(int i = 0; i < trades.size(); ++i) {
         if (((TradeOffer)trades.get(i)).getSellItem().getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   public void trade(int tradeIndex) {
      TradeOfferList trades = ((MerchantScreenHandler)this.handler).getRecipes();
      TradeOffer recipe = (TradeOffer)trades.get(tradeIndex);
      int safeguard = 0;

      while(
         !recipe.isDisabled()
            && this.client.player.currentScreenHandler.getCursorStack().isEmpty()
            && this.inputSlotsAreEmpty()
            && this.hasWantedTradeItems()
            && this.hasEnoughItemsInInventory(recipe)
            && this.canReceiveOutput(recipe.getSellItem())
      ) {
         this.transact(recipe);
         if (++safeguard > 50) {
            break;
         }
      }
   }

   private boolean hasWantedTradeItems() {
      return this.wantedTradeItems.size() > 0;
   }

   private boolean inputSlotsAreEmpty() {
      return ((MerchantScreenHandler)this.handler).getSlot(0).getStack().isEmpty()
         && ((MerchantScreenHandler)this.handler).getSlot(1).getStack().isEmpty()
         && ((MerchantScreenHandler)this.handler).getSlot(2).getStack().isEmpty();
   }

   private boolean hasEnoughItemsInInventory(TradeOffer recipe) {
      return !this.hasEnoughItemsInInventory(recipe.getAdjustedFirstBuyItem()) ? false : this.hasEnoughItemsInInventory(recipe.getSecondBuyItem());
   }

   private boolean hasEnoughItemsInInventory(ItemStack stack) {
      int remaining = stack.getCount();

      for(int i = ((MerchantScreenHandler)this.handler).slots.size() - 36; i < ((MerchantScreenHandler)this.handler).slots.size(); ++i) {
         ItemStack invstack = ((MerchantScreenHandler)this.handler).getSlot(i).getStack();
         if (invstack != null) {
            if (this.areItemStacksMergable(stack, invstack)) {
               remaining -= invstack.getCount();
            }

            if (remaining <= 0) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean canReceiveOutput(ItemStack stack) {
      int remaining = stack.getCount();

      for(int i = ((MerchantScreenHandler)this.handler).slots.size() - 36; i < ((MerchantScreenHandler)this.handler).slots.size(); ++i) {
         ItemStack invstack = ((MerchantScreenHandler)this.handler).getSlot(i).getStack();
         if (invstack == null || invstack.isEmpty()) {
            return true;
         }

         if (this.areItemStacksMergable(stack, invstack) && stack.getMaxCount() >= stack.getCount() + invstack.getCount()) {
            remaining -= invstack.getMaxCount() - invstack.getCount();
         }

         if (remaining <= 0) {
            return true;
         }
      }

      return false;
   }

   private void transact(TradeOffer recipe) {
      int putback0 = this.fillSlot(0, recipe.getAdjustedFirstBuyItem());
      int putback1 = this.fillSlot(1, recipe.getSecondBuyItem());
      this.getslot(putback0, putback1);
      if (putback0 != -1) {
         this.slotShiftClick(0);
      }

      if (putback1 != -1) {
         this.slotShiftClick(1);
      }

      this.onMouseClick(null, 0, 99, SlotActionType.SWAP);
   }

   private int fillSlot(int slot, ItemStack stack) {
      int remaining = stack.getCount();
      if (remaining == 0) {
         return -1;
      } else {
         for(int i = ((MerchantScreenHandler)this.handler).slots.size() - 36; i < ((MerchantScreenHandler)this.handler).slots.size(); ++i) {
            ItemStack invstack = ((MerchantScreenHandler)this.handler).getSlot(i).getStack();
            if (invstack != null) {
               boolean needPutBack = false;
               if (this.areItemStacksMergable(stack, invstack)) {
                  if (stack.getCount() + invstack.getCount() > stack.getMaxCount()) {
                     needPutBack = true;
                  }

                  remaining -= invstack.getCount();
                  this.slotClick(i);
                  this.slotClick(slot);
               }

               if (needPutBack) {
                  this.slotClick(i);
               }

               if (remaining <= 0) {
                  return remaining < 0 ? i : -1;
               }
            }
         }

         return -1;
      }
   }

   private boolean areItemStacksMergable(ItemStack a, ItemStack b) {
      if (a != null && b != null) {
         return a.getItem() == b.getItem() && (!a.isDamageable() || a.getDamage() == b.getDamage()) && ItemStack.areNbtEqual(a, b);
      } else {
         return false;
      }
   }

   private void getslot(int... forbidden) {
      this.slotShiftClick(2);

      for(int i = ((MerchantScreenHandler)this.handler).slots.size() - 36; i < ((MerchantScreenHandler)this.handler).slots.size(); ++i) {
         boolean isForbidden = false;

         for(int f : forbidden) {
            if (i == f) {
               isForbidden = true;
               break;
            }
         }

         if (!isForbidden) {
            ItemStack invstack = ((MerchantScreenHandler)this.handler).getSlot(i).getStack();
            if (invstack == null || invstack.isEmpty()) {
               this.slotClick(i);
               return;
            }
         }
      }
   }

   public void slotClick(int slot) {
      this.onMouseClick(null, slot, 0, SlotActionType.PICKUP);
   }

   private void slotShiftClick(int slot) {
      this.onMouseClick(null, slot, 0, SlotActionType.QUICK_MOVE);
   }
}
