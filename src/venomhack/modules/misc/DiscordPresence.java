package venomhack.modules.misc;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.Utils;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class DiscordPresence extends ModuleHelper {
   private final Setting<String> line1 = this.setting(
      "line-1", "Text on line 1 of the RPC.", "{server}", this.sgGeneral, null, booleanSetting -> this.updateDetails()
   );
   private final Setting<String> line2 = this.setting(
      "line-2", "Text on line 2 of the RPC.", "Venomhack on top!", this.sgGeneral, null, booleanSetting -> this.updateDetails()
   );
   private final Setting<DiscordPresence.Logo> logo = this.setting(
      "logo", "Which logo to use.", DiscordPresence.Logo.SIMPLE, this.sgGeneral, null, logo1 -> this.setLogo()
   );
   private final Setting<Boolean> transparent = this.setting(
      "transparent", "Removes the image background color.", Boolean.valueOf(false), this.sgGeneral, null, logo1 -> this.setLogo()
   );
   private static final RichPresence RPC = new RichPresence();

   public DiscordPresence() {
      super(Venomhack420.CATEGORY, "discord-rpc", "Displays a Venomhack RPC.");
   }

   public void onActivate() {
      DiscordIPC.start(866847445052817469L, null);
      RPC.setStart(System.currentTimeMillis() / 1000L);
      this.setLogo();
      this.updateDetails();
   }

   public void onDeactivate() {
      DiscordIPC.stop();
   }

   private String getLine(Setting<String> line) {
      return ((String)line.get()).length() > 0 ? ((String)line.get()).replace("{player}", this.getName()).replace("{server}", this.getServer()) : null;
   }

   private String getServer() {
      return this.mc.isInSingleplayer() ? "Singleplayer" : Utils.getWorldName();
   }

   private String getName() {
      return this.mc.getSession().getUsername();
   }

   private void updateDetails() {
      RPC.setDetails(this.getLine(this.line1));
      RPC.setState(this.getLine(this.line2));
      DiscordIPC.setActivity(RPC);
   }

   private void setLogo() {
      StringBuilder key = new StringBuilder();

      key.append(switch((DiscordPresence.Logo)this.logo.get()) {
         case SHEIT -> "sheit";
         case BLACK -> "black";
         case WHITE -> "white";
         default -> "simple";
      });
      if (this.transparent.get()) {
         key.append("2");
      }

      RPC.setLargeImage(key.toString(), "Venomhack420 " + Venomhack420.VERSION);
   }

   public static enum Logo {
      BLACK("Black"),
      WHITE("White"),
      SIMPLE("Simple"),
      SHEIT("Sheeiiiitttttt");

      private final String title;

      private Logo(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
