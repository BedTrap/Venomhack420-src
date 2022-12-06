package venomhack.modules.chat;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Formatting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameMode;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;
import venomhack.Venomhack420;
import venomhack.gui.screens.GuideScreen;
import venomhack.modules.ModuleHelper;
import venomhack.utils.PlayerUtils2;
import venomhack.utils.RandUtils;
import venomhack.utils.TextUtils;

public class ArmorMessage extends ModuleHelper {
   private final Setting<Boolean> toastNotification = this.setting(
      "toast", "Will send the notification for your own armor as a toast instead of a chat message.", Boolean.valueOf(false)
   );
   private final Setting<List<SoundEvent>> sound = this.setting(
      "sound", "The sound it makes when the toast pops up.", this.sgGeneral, this.toastNotification::get, soundEvents -> {
         if (soundEvents.size() > 1) {
            soundEvents.remove(0);
         }
      }, new SoundEvent[]{SoundEvents.ENTITY_ITEM_BREAK}
   );
   private final Setting<Integer> duration = this.setting(
      "toast-duration", "For how long the toast should show up on your screen in seconds.", Integer.valueOf(6), this.toastNotification::get
   );
   private final Setting<Boolean> friends = this.setting("friends", "Whether to send a notification to friends or not.", Boolean.valueOf(true));
   private final Setting<Integer> threshold = this.setting(
      "durability-threshold", "At what durability to notify.", Integer.valueOf(25), this.sgGeneral, 1.0, 100.0, 1, 100
   );
   private final Setting<String> message = this.setting(
      "message", "defines the message to send when armor runs low", "Your {piece} {grammar} low on durability! ({percent}%)"
   );
   private final Int2IntMap armor = new Int2IntOpenHashMap();

   public ArmorMessage() {
      super(Venomhack420.CATEGORY, "armor-message", "Sends a message in chat when your or your friends armor runs low.");
   }

   @EventHandler
   private void onTick(Post event) {
      for(Entity entity : this.mc.world.getEntities()) {
         if (entity instanceof PlayerEntity player
            && (player.equals(this.mc.player) || this.friends.get() && !Friends.get().shouldAttack(player))
            && PlayerUtils2.getGameMode(player) != GameMode.CREATIVE
            && PlayerUtils2.getGameMode(player) != GameMode.SPECTATOR) {
            for(ItemStack itemStack : player.getArmorItems()) {
               if (itemStack != null && !itemStack.isEmpty() && itemStack.isDamageable()) {
                  int index = player.getId();
                  int damage = (int)RandUtils.durabilityPercentage(itemStack);
                  String piece = "";
                  String grammar = "";
                  if (itemStack.getItem() instanceof ElytraItem) {
                     index *= 3;
                     piece = "Elytra";
                     grammar = "is";
                  } else {
                     Item slotType = itemStack.getItem();
                     if (slotType instanceof ArmorItem armorItem) {
                        EquipmentSlot slotTypex = armorItem.getSlotType();
                        index *= slotTypex.getArmorStandSlotId();
                        switch(slotTypex) {
                           case FEET:
                              piece = "Boots";
                              grammar = "are";
                              break;
                           case LEGS:
                              piece = "Leggings";
                              grammar = "are";
                              break;
                           case CHEST:
                              piece = "Chestplate";
                              grammar = "is";
                              break;
                           default:
                              piece = "Helmet";
                              grammar = "is";
                        }
                     }
                  }

                  if (!(damage > this.threshold.get() | this.armor.put(index, damage) <= this.threshold.get())) {
                     String msg = ((String)this.message.get())
                        .replace("{piece}", piece)
                        .replace("{grammar}", grammar)
                        .replace("{percent}", damage + "")
                        .replace("{player}", player.getEntityName());
                     if (player == this.mc.player) {
                        if (this.toastNotification.get()) {
                           this.mc
                              .getToastManager()
                              .add(
                                 new ArmorMessage.ArmorNotificationToast(itemStack.getItem(), "Your " + piece + " " + grammar + " on " + damage + "%")
                              );
                        } else {
                           this.info(index, Text.literal(msg).formatted(Formatting.YELLOW));
                        }
                     } else {
                        TextUtils.sendNewMessage("/msg " + player.getEntityName() + " " + msg);
                     }
                  }
               }
            }
         }
      }
   }

   public WWidget getWidget(GuiTheme theme) {
      WVerticalList list = theme.verticalList();
      WButton placeholders = (WButton)list.add(theme.button("Placeholders")).expandX().widget();
      placeholders.action = () -> new GuideScreen()
            .withContents(
               "These are the placeholders for armor message.",
               "They can't be modified and only work for this module.",
               "",
               "{piece} gets replaced for the armor piece that dropped below the durability threshold.",
               "{grammar} returns 'is' or 'are', depending on the type of armor.",
               "{percent} just returns the percentage value of the durability of the armor piece.",
               "{player} will return the player whos armor is running low.",
               ""
            )
            .show();
      return list;
   }

   private class ArmorNotificationToast extends MeteorToast {
      public ArmorNotificationToast(@Nullable Item item, @Nullable String text) {
         super(item, ArmorMessage.this.title(), text, (long)(ArmorMessage.this.duration.get() * 1000));
      }

      public SoundInstance getSound() {
         return PositionedSoundInstance.master(
            ((List)ArmorMessage.this.sound.get()).isEmpty() ? SoundEvents.ENTITY_ITEM_BREAK : (SoundEvent)((List)ArmorMessage.this.sound.get()).get(0), 1.2F, 1.0F
         );
      }
   }
}
