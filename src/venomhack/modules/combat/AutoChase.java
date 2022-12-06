package venomhack.modules.combat;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;
import venomhack.utils.PlayerUtils2;

public class AutoChase extends ModuleHelper {
   private final Setting<Integer> range = this.setting("range", "The range in which players will be followed.", Integer.valueOf(6), this.sgGeneral, 1.0, 10.0);
   private final Setting<Double> yOffset = this.setting(
      "y offset", "The offset relative to the target's position you will be kept at.", 1.0, this.sgGeneral, 0.0, 3.0, 2
   );
   private final List<PlayerEntity> players = new ArrayList();

   public AutoChase() {
      super(Venomhack420.CATEGORY, "Auto Chase", "ungabunga automatically chases wasping opponents");
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.mc.player.isFallFlying()) {
         if (this.players.isEmpty()) {
            return;
         }

         Vec3d closestPlayerPos = ((PlayerEntity)this.players.get(0)).getPos();
         this.mc.player.setPos(closestPlayerPos.x, closestPlayerPos.y + this.yOffset.get(), closestPlayerPos.z);
         this.mc.player.setVelocity(0.0, 0.0, 0.0);
      }
   }

   @EventHandler
   private void onTickPost(Post event) {
      PlayerUtils2.collectTargets(this.players, null, this.range.get(), 256, false, false, false, SortPriority.LowestDistance);
   }
}
