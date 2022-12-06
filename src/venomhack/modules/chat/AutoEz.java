package venomhack.modules.chat;

import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import venomhack.Venomhack420;
import venomhack.events.PlayerDeathEvent;
import venomhack.gui.screens.GuideScreen;
import venomhack.modules.ModuleHelper;
import venomhack.utils.TextUtils;

public class AutoEz extends ModuleHelper {
   private final SettingGroup sgMessages = this.settings.createGroup("EZ Messages");
   private final SettingGroup sgKillstreak = this.settings.createGroup("Killstreak");
   private final Setting<Boolean> dms = this.setting(
      "direct-messages", "Whether to send the ez message as a dm to the victim or not.", Boolean.valueOf(false), this.sgGeneral
   );
   private final Setting<Integer> delay = this.setting(
      "delay", "How many ticks between being able to ez someone.", Integer.valueOf(5), this.sgGeneral, 0.0, 20.0
   );
   private final Setting<Boolean> chat = this.setting(
      "only-on-kill-messages", "Will only send an ez message when you get the kill message in chat", Boolean.valueOf(false), this.sgGeneral
   );
   private final Setting<List<String>> messages = this.setting(
      "",
      "A random message will be chosen to humiliate your victims with.",
      this.sgMessages,
      null,
      new String[]{
         "{player} died to the power of Venomhack420!",
         "You can tell {player} isn't part of Venomforce:  https://discord.gg/VqRd4MJkbY",
         "EZ {player}. Venomhack owns me and all!"
      }
   );
   private final Setting<Boolean> addKsSuffix = this.setting(
      "always-add-killstreak", "Will append your current killstreak to all your ez messages automatically.", Boolean.valueOf(false), this.sgKillstreak
   );
   private final Setting<String> ksSuffixMsg = this.setting(
      "killstreak-suffix-message",
      "Use {ks} for the number of kills and {ksSuffix} for the ending like st, nd, rd or th.",
      " | {ks}{ksSuffix} kill in a row!",
      this.sgKillstreak,
      this.addKsSuffix::get
   );
   private final Setting<Boolean> sayKillStreak = this.setting(
      "announce-killstreak", "Will send a special message each X kills instead of a normal ez one.", Boolean.valueOf(true), this.sgKillstreak
   );
   private final Setting<Integer> ksCount = this.setting(
      "each-X-messages", "After how many X kills to send the killstreak message.", Integer.valueOf(5), this.sgKillstreak, this.sayKillStreak::get, 0.0, 10.0
   );
   private final Setting<String> ksMsg = this.setting(
      "killstreak-message",
      "Use {ks} for the number of kills and {ksSuffix} for the ending like st, nd, rd or th.",
      "EZ. {ks}{ksSuffix} kill in a row. Venomhack on top.",
      this.sgKillstreak,
      this.sayKillStreak::get
   );
   private int delayLeft;

   public AutoEz() {
      super(Venomhack420.CATEGORY, "AutoEZ", "Automatically sends a message in chat when you kill someone.");
   }

   public void onActivate() {
      this.delayLeft = 0;
   }

   @EventHandler
   private void onTick(Post event) {
      --this.delayLeft;
   }

   @EventHandler(
      priority = 100
   )
   private void onDeath(PlayerDeathEvent event) {
      PlayerEntity victim = event.getPlayer();
      if (this.delayLeft <= 0) {
         if (event.isTarget()) {
            if (!this.chat.get() || event.isChat()) {
               if (((List)this.messages.get()).isEmpty()) {
                  this.warning("You have no messages set for Auto eZ.", new Object[0]);
               } else {
                  this.delayLeft = this.delay.get();
                  StringBuilder msg = new StringBuilder();
                  msg.append(TextUtils.getNewMessage((List<String>)this.messages.get()));
                  if (this.sayKillStreak.get() && Math.max(1, Venomhack420.STATS.getKillStreak()) % this.ksCount.get() == 0) {
                     msg.delete(0, msg.length());
                     msg.append((String)this.ksMsg.get());
                  } else if (this.addKsSuffix.get()) {
                     msg.append((String)this.ksSuffixMsg.get());
                  }

                  if (this.dms.get()) {
                     msg.insert(0, "/msg " + victim.getGameProfile().getName() + " ");
                  }

                  TextUtils.sendNewMessage(
                     msg.toString()
                        .replace("{player}", victim.getGameProfile().getName())
                        .replace("{pops}", Venomhack420.STATS.getTotemPops(victim) + "")
                        .replace("{totem}", Venomhack420.STATS.getTotemPops(victim) == 1 ? "totem" : "totems")
                  );
               }
            }
         }
      }
   }

   public WWidget getWidget(GuiTheme theme) {
      WVerticalList list = theme.verticalList();
      WButton placeholders = (WButton)list.add(theme.button("Placeholders")).expandX().widget();
      placeholders.action = () -> new GuideScreen().show();
      return list;
   }
}
