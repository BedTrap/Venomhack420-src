package venomhack.modules.world;

import java.util.List;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.PeekScreen;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;
import venomhack.utils.RandUtils;

public class ItemDropper extends ModuleHelper {
   private final SettingGroup sgKeyBinds = this.settings.createGroup("Keybinds");
   private final SettingGroup sgToggles = this.settings.createGroup("Toggles");
   private final Setting<Boolean> enableChestClearing = this.setting(
      "enable-chest-clearing", "enables dropping/deleting items from chests", Boolean.valueOf(true), this.sgToggles
   );
   private final Setting<Boolean> enableInventoryClearing = this.setting(
      "enable-inventory-clearing", "enables dropping/deleting items from inventory", Boolean.valueOf(true), this.sgToggles
   );
   private final Setting<ItemDropper.Mode> clearMode = this.setting(
      "clear-mode", "Type of action. WARNING: DELETE MODE DELETES SELECTED ITEMS FROM THE GUI", ItemDropper.Mode.DROP
   );
   private final Setting<List<Item>> items = this.setting(
      "items", "Which items to drop", this.sgGeneral, false, item -> true, null, null, null, new Item[]{Items.AIR}
   );
   private final Setting<Keybind> containerClearKey = this.setting("container-clear-key", "Key to clear items.", Keybind.fromButton(2), this.sgKeyBinds);
   private final Setting<ItemDropper.ActivationMode> inventoryClearMode = this.setting(
      "inventory-drop-mode", "When to drop items from the inventory.", ItemDropper.ActivationMode.ON_KEY
   );
   private final Setting<Keybind> inventoryClearKey = this.setting(
      "inventory-clear-key", "Key to drop items from the inventory.", Keybind.fromKey(82), this.sgKeyBinds
   );

   public ItemDropper() {
      super(Venomhack420.CATEGORY, "container-cleaner", "drops/deletes selected items leaving only the ones you want.");
   }

   @EventHandler
   private void onMouseButton(MouseButtonEvent event) {
      if (event.action == KeyAction.Press) {
         if (((Keybind)this.containerClearKey.get()).matches(false, event.button)) {
            this.doChestDrops();
         }

         if (this.inventoryClearMode.get() == ItemDropper.ActivationMode.ON_KEY && ((Keybind)this.inventoryClearKey.get()).matches(false, event.button)) {
            this.doInventoryDrops();
         }
      }
   }

   @EventHandler
   private void onKey(KeyEvent event) {
      if (event.action == KeyAction.Press) {
         if (((Keybind)this.containerClearKey.get()).matches(true, event.key)) {
            this.doChestDrops();
         }

         if (this.inventoryClearMode.get() == ItemDropper.ActivationMode.ON_KEY && ((Keybind)this.inventoryClearKey.get()).matches(true, event.key)) {
            this.doInventoryDrops();
         }
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.inventoryClearMode.get() == ItemDropper.ActivationMode.ALWAYS) {
         this.doInventoryDrops();
      }
   }

   private void doChestDrops() {
      if (Utils.canUpdate() && this.enableChestClearing.get()) {
         Screen slots = this.mc.currentScreen;
         if (slots instanceof HandledScreen screen && !(screen instanceof PeekScreen) && !(screen instanceof InventoryScreen)) {
            DefaultedList<Slot> slotsx = screen.getScreenHandler().slots;

            for(int i = 0; i < slotsx.size() && i != slotsx.size() - 36; ++i) {
               Item slotItem = ((Slot)slotsx.get(i)).getStack().getItem();
               if (((List)this.items.get()).contains(slotItem) && slotItem != Items.AIR) {
                  this.sendClickPacket(i);
               }
            }

            return;
         }
      }
   }

   private void doInventoryDrops() {
      if (Utils.canUpdate() && this.enableInventoryClearing.get()) {
         ScreenHandler slots = this.mc.player.currentScreenHandler;
         if (slots instanceof PlayerScreenHandler screen) {
            DefaultedList var5 = screen.slots;

            for(int i = 0; i < var5.size(); ++i) {
               if (i >= 9) {
                  Item slotItem = ((Slot)var5.get(i)).getStack().getItem();
                  if (((List)this.items.get()).contains(slotItem) && slotItem != Items.AIR) {
                     this.sendClickPacket(i);
                  }
               }
            }
         }
      }
   }

   private void sendClickPacket(int index) {
      boolean drop = this.clearMode.get() == ItemDropper.Mode.DROP;
      int button = drop ? 1 : 69;
      SlotActionType type = drop ? SlotActionType.THROW : SlotActionType.SWAP;
      RandUtils.clickSlotPacket(index, button, type);
   }

   public static enum ActivationMode {
      ALWAYS,
      ON_KEY;
   }

   public static enum Mode {
      DROP,
      DELETE;
   }
}
