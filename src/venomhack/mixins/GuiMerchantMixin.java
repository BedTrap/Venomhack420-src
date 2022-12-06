package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.world.villager_trader.BetterGuiMerchant;
import venomhack.modules.world.villager_trader.VillagerTrader;

@Mixin({HandledScreens.class})
public abstract class GuiMerchantMixin {
   @Inject(
      method = {"open"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void displayVillagerTradeGui(ScreenHandlerType<?> type, MinecraftClient client, int any, Text component, CallbackInfo ci) {
      if (type == ScreenHandlerType.MERCHANT) {
         VillagerTrader villagerTrader = (VillagerTrader)Modules.get().get(VillagerTrader.class);
         if (villagerTrader.isActive()) {
            MerchantScreenHandler container = (MerchantScreenHandler)ScreenHandlerType.MERCHANT.create(any, client.player.getInventory());
            BetterGuiMerchant screen = villagerTrader.initVillagerTrader(container, client.player.getInventory(), component);
            client.player.currentScreenHandler = container;
            client.setScreen(screen);
            ci.cancel();
         }
      }
   }
}
