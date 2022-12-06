package venomhack.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;
import venomhack.utils.TextUtils;

public class ObsidianFarm extends ModuleHelper {
   private final Setting<String> message = this.setting("message", "The message / command to execute after switching dimensions.", "/schinken");
   private final Setting<ObsidianFarm.When> when = this.setting("when", "When to activate.", ObsidianFarm.When.DYNAMIC);
   private boolean t;
   private Dimension dim;
   private Dimension switchDim;
   private int count = 0;

   public ObsidianFarm() {
      super(Venomhack420.CATEGORY, "obsidian-farm", "Automatically performs a set command on dimension change to speed up obsidian farms.");
   }

   @EventHandler
   public void onActivate() {
      if (this.mc.player != null) {
         this.dim = PlayerUtils.getDimension();
      } else {
         this.dim = Dimension.Overworld;
      }
   }

   @EventHandler
   private void onTick(Pre event) {
      switch((ObsidianFarm.When)this.when.get()) {
         case OVERWORLD:
            this.switchDim = Dimension.Overworld;
            break;
         case NETHER:
            this.switchDim = Dimension.Nether;
            break;
         case END:
            this.switchDim = Dimension.End;
            break;
         case DYNAMIC:
            if (this.count > 0 && this.mc.player != null) {
               if (PlayerUtils.getDimension() != this.dim) {
                  TextUtils.sendNewMessage((String)this.message.get());
               }

               this.count = 0;
               this.dim = PlayerUtils.getDimension();
            } else {
               ++this.count;
            }
      }

      if (PlayerUtils.getDimension() == this.switchDim && this.t) {
         TextUtils.sendNewMessage((String)this.message.get());
         this.t = false;
      } else {
         if (PlayerUtils.getDimension() != this.switchDim) {
            this.t = true;
         }
      }
   }

   public static enum When {
      OVERWORLD("Overworld"),
      NETHER("Nether"),
      END("End"),
      DYNAMIC("Dynamic");

      private final String title;

      private When(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
