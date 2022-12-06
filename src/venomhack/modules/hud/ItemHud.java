package venomhack.modules.hud;

import java.util.List;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.ItemListSetting.Builder;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import venomhack.Venomhack420;

public class ItemHud extends HudElement {
   public static final HudElementInfo<ItemHud> INFO = new HudElementInfo(
      Venomhack420.HUD_GROUP, "item-hud", "Displays the amount of items in your inventory.", ItemHud::new
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<Item>> items = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("items")).description("Which modules to display."))
            .defaultValue(new Item[]{Items.EXPERIENCE_BOTTLE, Items.ENCHANTED_GOLDEN_APPLE, Items.END_CRYSTAL, Items.OBSIDIAN})
            .build()
      );
   private final Setting<ItemHud.Orientation> orientation = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder(
                        
                     )
                     .name("orientation"))
                  .description("How to display items."))
               .defaultValue(ItemHud.Orientation.Horizontal))
            .build()
      );
   private final Setting<Double> spacing = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder(
                     
                  )
                  .name("spacing"))
               .description("The spacing between items."))
            .defaultValue(0.0)
            .min(0.0)
            .sliderMin(0.0)
            .sliderMax(16.0)
            .build()
      );
   private final Setting<Double> scale = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder(
                     
                  )
                  .name("scale"))
               .description("The scale."))
            .defaultValue(2.0)
            .min(1.0)
            .sliderMin(1.0)
            .sliderMax(5.0)
            .build()
      );

   public ItemHud() {
      super(INFO);
   }

   public void tick(HudRenderer renderer) {
      switch((ItemHud.Orientation)this.orientation.get()) {
         case Horizontal:
            this.box
               .setSize(
                  (this.spacing.get() + 16.0) * (double)((List)this.items.get()).size() * this.scale.get() - this.spacing.get() * this.scale.get(),
                  16.0 * this.scale.get()
               );
            break;
         case Vertical:
            this.box
               .setSize(
                  16.0 * this.scale.get(),
                  (this.spacing.get() + 16.0) * (double)((List)this.items.get()).size() * this.scale.get() - this.spacing.get() * this.scale.get()
               );
      }
   }

   public void render(HudRenderer renderer) {
      double x = (double)this.box.x;
      double y = (double)this.box.y;

      for(int i = 0; i < ((List)this.items.get()).size(); ++i) {
         switch((ItemHud.Orientation)this.orientation.get()) {
            case Horizontal:
               x = (double)this.box.x + (this.spacing.get() + 16.0) * (double)i * this.scale.get();
               break;
            case Vertical:
               y = (double)this.box.y + (this.spacing.get() + 16.0) * (double)i * this.scale.get();
         }

         if (this.isInEditor()) {
            RenderUtils.drawItem(((Item)((List)this.items.get()).get(i)).getDefaultStack(), (int)x, (int)y, this.scale.get(), true);
         } else if (InvUtils.find(new Item[]{(Item)((List)this.items.get()).get(i)}).count() > 0) {
            RenderUtils.drawItem(
               new ItemStack(
                  (ItemConvertible)((List)this.items.get()).get(i), InvUtils.find(new Item[]{(Item)((List)this.items.get()).get(i)}).count()
               ),
               (int)x,
               (int)y,
               this.scale.get(),
               true
            );
         }
      }
   }

   public static enum Orientation {
      Horizontal,
      Vertical;
   }
}
