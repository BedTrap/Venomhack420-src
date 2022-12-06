package venomhack.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class TotemLog extends ModuleHelper {
   private final Setting<Integer> totems = this.setting("totem-amount", "The threshold to disconnect at.", Integer.valueOf(1));
   private final Setting<Boolean> toggleOff = this.setting("toggle-off", "Disables this after usage.", Boolean.valueOf(true));

   public TotemLog() {
      super(Venomhack420.CATEGORY, "totem-log", "Automatically disconnects you when you drop to a certain totem count.");
   }

   @EventHandler
   private void onTick(Post event) {
      if (InvUtils.find(new Item[]{Items.TOTEM_OF_UNDYING}).count() <= this.totems.get()) {
         this.mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[AutoLog] You have " + this.totems.get() + " totems left!")));
         if (this.toggleOff.get()) {
            this.toggle();
         }
      }
   }
}
