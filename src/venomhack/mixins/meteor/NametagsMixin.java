package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameMode;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(
   value = {Nametags.class},
   remap = false
)
public class NametagsMixin {
   @Shadow
   @Final
   private SettingGroup sgPlayers;
   @Unique
   private Setting<Boolean> renderShulkerItems = null;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")},
      remap = false
   )
   private void onInit(CallbackInfo ci) {
      this.renderShulkerItems = this.sgPlayers
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("render-shulker-items")).description("Whether to render shulker items or not."))
                  .defaultValue(true))
               .build()
         );
   }

   @Inject(
      method = {"renderNametagPlayer"},
      at = {@At(
   value = "INVOKE",
   target = "Lmeteordevelopment/meteorclient/utils/render/RenderUtils;drawItem(Lnet/minecraft/item/ItemStack;IIDZ)V",
   shift = Shift.AFTER
)},
      locals = LocalCapture.CAPTURE_FAILSOFT,
      remap = true
   )
   private void renderNametagPlayer(
      PlayerEntity player,
      boolean shadow,
      CallbackInfo ci,
      TextRenderer text,
      GameMode gm,
      String gmText,
      String name,
      Color nameColor,
      float absorption,
      int health,
      double healthPercentage,
      String healthText,
      Color healthColor,
      int ping,
      String pingText,
      double dist,
      String distText,
      double gmWidth,
      double nameWidth,
      double healthWidth,
      double pingWidth,
      double distWidth,
      double width,
      double widthHalf,
      double heightDown,
      double hX,
      double hY,
      boolean hasItems,
      int maxEnchantCount,
      double itemsHeight,
      double itemWidthTotal,
      double itemWidthHalf,
      double y,
      double x,
      int i,
      ItemStack stack
   ) {
      if (this.renderShulkerItems.get()) {
         if (Utils.isShulker(stack.getItem())) {
            int offsetX = 176;
            int offsetY = 67;
            ItemStack[] stackItems = new ItemStack[27];
            Utils.getItemsInContainerItem(stack, stackItems);
            int itemIndex = 0;

            for(int row = 0; row < 3; ++row) {
               for(int slot = 0; slot < 9; ++itemIndex) {
                  int itemX = (int)x + 8 + slot * 18;
                  int itemY = (int)y + 7 + row * 18;
                  if (this.missingArmor(player) && player.getOffHandStack() == stack) {
                     RenderUtils.drawItem(stackItems[itemIndex], offsetX - itemX, itemY - offsetY, 1.0, true);
                  } else {
                     RenderUtils.drawItem(stackItems[itemIndex], itemX - offsetX, itemY - offsetY, 1.0, true);
                  }

                  ++slot;
               }
            }
         }
      }
   }

   @Unique
   private boolean missingArmor(PlayerEntity player) {
      DefaultedList<ItemStack> armor = player.getInventory().armor;
      return ((ItemStack)armor.get(0)).isEmpty()
         || ((ItemStack)armor.get(1)).isEmpty()
         || ((ItemStack)armor.get(2)).isEmpty()
         || ((ItemStack)armor.get(3)).isEmpty();
   }
}
