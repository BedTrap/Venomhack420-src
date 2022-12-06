package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.item.Item;
import net.minecraft.village.TradeOfferList;
import net.minecraft.text.Text;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.world.villager_trader.BetterGuiMerchant;
import venomhack.modules.world.villager_trader.VillagerTrader;

@Mixin({MerchantScreen.class})
public abstract class MerchantScreenMixin extends HandledScreen<MerchantScreenHandler> {
   @Shadow
   private int selectedIndex;

   public MerchantScreenMixin(MerchantScreenHandler merchantContainer_1, PlayerInventory playerInventory_1, Text text_1) {
      super(merchantContainer_1, playerInventory_1, text_1);
   }

   @Inject(
      method = {"renderScrollbar"},
      at = {@At("RETURN")}
   )
   private void onRenderScrollbar(MatrixStack matrices, int x, int y, TradeOfferList tradeOffers, CallbackInfo ci) {
      VillagerTrader villagerTrader = (VillagerTrader)Modules.get().get(VillagerTrader.class);
      if (villagerTrader.isActive()) {
         MerchantScreenAccessor mrcntscrn = (MerchantScreenAccessor)this;

         for(Item item : villagerTrader.getWantedTradeItems()) {
            int tradeIndex = ((BetterGuiMerchant)this).getWantedItemIndex(item);
            if (tradeIndex != -1) {
               mrcntscrn.setSelectedIndex(tradeIndex);
               mrcntscrn.invokeSyncRecipeIndex();
            }
         }

         this.close();
      }
   }

   @Inject(
      method = {"syncRecipeIndex"},
      at = {@At("RETURN")}
   )
   public void tradeOnSetRecipeIndex(CallbackInfo ci) {
      VillagerTrader villagerTrader = (VillagerTrader)Modules.get().get(VillagerTrader.class);
      if (villagerTrader.isActive()) {
         this.onMouseClick(null, 0, 0, SlotActionType.QUICK_MOVE);
         this.onMouseClick(null, 1, 0, SlotActionType.QUICK_MOVE);
         ((BetterGuiMerchant)this).trade(this.selectedIndex);
      }
   }
}
