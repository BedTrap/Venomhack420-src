package venomhack.modules.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.ItemSetting.Builder;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class AutoSort extends ModuleHelper {
   private final SettingGroup sgHotbar = this.settings.createGroup("Hotbar");
   private final SettingGroup sgInvRow1 = this.settings.createGroup("Inventory Row 1");
   private final SettingGroup sgInvRow2 = this.settings.createGroup("Inventory Row 2");
   private final SettingGroup sgInvRow3 = this.settings.createGroup("Inventory Row 3");
   private final Setting<Integer> delay = this.setting("delay", "Delay between sorting items.", Integer.valueOf(2), this.sgGeneral, 0.0, 20.0);
   private final List<Setting<Item>> settingList = this.initSettings();
   private int begin;
   private int delayLeft;

   public AutoSort() {
      super(Venomhack420.CATEGORY, "auto-sort", "Sorts your inventory. May cause desync on some servers.");
   }

   public void onActivate() {
      this.begin = 0;
      this.delayLeft = 0;
   }

   @EventHandler
   private void onTick(Post event) {
      --this.delayLeft;
      if (this.delayLeft <= 0) {
         if (this.delay.get() == 0) {
            this.instantMoveItems();
         } else {
            this.moveItems();
         }

         ++this.begin;
      }
   }

   private List<Setting<Item>> initSettings() {
      ArrayList<Setting<Item>> settings = new ArrayList();
      String baseName = "inv-slot-";
      String baseDescription = "Inventory slot ";
      int cat = 0;

      for(int i = 0; i < 36; ++i) {
         if (i % 9 == 0 && i != 0) {
            ++cat;
         }

         ItemSetting itemSetting = ((Builder)((Builder)((Builder)new Builder().name(baseName + i)).description(baseDescription + i))
               .defaultValue(Items.AIR))
            .build();
         switch(cat) {
            case 0:
               settings.add(this.sgHotbar.add(itemSetting));
               break;
            case 1:
               settings.add(this.sgInvRow1.add(itemSetting));
               break;
            case 2:
               settings.add(this.sgInvRow2.add(itemSetting));
               break;
            case 3:
               settings.add(this.sgInvRow3.add(itemSetting));
         }
      }

      return settings;
   }

   private FindItemResult searchInv(Item item) {
      return this.find(itemStack -> itemStack.getItem() == item);
   }

   private FindItemResult find(Predicate<ItemStack> isGood) {
      return this.find(isGood, this.begin, 36);
   }

   private FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
      int slot = -1;
      int count = 0;

      for(int i = start; i <= end; ++i) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         if (isGood.test(stack)) {
            if (slot == -1) {
               slot = i;
            }

            count += stack.getCount();
         }
      }

      return new FindItemResult(slot, count);
   }

   private void moveItems() {
      Setting<Item> invSlot = (Setting)this.settingList.get(this.begin);
      if (invSlot.get() != Items.AIR && this.mc.player.getInventory().getStack(this.begin).getItem() != invSlot.get()) {
         FindItemResult getSlot = this.searchInv((Item)invSlot.get());
         InvUtils.move().from(getSlot.slot()).to(this.begin);
         this.delayLeft = this.delay.get();
      }

      if (this.begin == 35) {
         this.toggle();
      }
   }

   private void instantMoveItems() {
      for(int i = 0; i < 36; ++i) {
         Setting<Item> invSlot = (Setting)this.settingList.get(i);
         if (invSlot.get() != Items.AIR && this.mc.player.getInventory().getStack(i).getItem() != invSlot.get()) {
            int slot = this.searchInv((Item)invSlot.get()).slot();
            InvUtils.move().from(slot).to(i);
         }
      }

      this.toggle();
   }
}
