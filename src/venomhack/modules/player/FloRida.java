package venomhack.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Quiver;
import meteordevelopment.meteorclient.systems.modules.player.EXPThrower;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class FloRida extends ModuleHelper {
   private final Setting<Integer> speed = this.setting("rotation-speed", "The speed at which you rotate", Integer.valueOf(20), this.sgGeneral, 0.0, 50.0);
   private int count = 0;

   public FloRida() {
      super(Venomhack420.CATEGORY, "flo-rida", "Makes you spin right round ;)");
   }

   @EventHandler
   public void onTick(Post event) {
      Modules modules = Modules.get();
      if (!modules.isActive(EXPThrower.class) && !modules.isActive(Quiver.class) && !modules.isActive(XpThrower.class)) {
         this.count += this.speed.get();
         if (this.count > 180) {
            this.count -= 360;
         }

         Rotations.rotate((double)this.count, 0.0);
      }
   }
}
