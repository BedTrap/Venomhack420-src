package venomhack.modules.world;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import venomhack.Venomhack420;

public class WaypointDeleter extends Module {
   public WaypointDeleter() {
      super(Venomhack420.CATEGORY, "waypoint-deleter", "Deletes waypoints that are on chests when you interact with it. Useful for the egap finder.");
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      if (event.screen instanceof GenericContainerScreen) {
         for(Waypoint waypoint : Waypoints.get()) {
            int x = waypoint.getPos().getX() + 1;
            int y = waypoint.getPos().getY();
            int z = waypoint.getPos().getZ() + 1;
            Vec3d target = this.mc.crosshairTarget.getPos();
            if (x == (int)target.x && y == (int)target.y && z == (int)target.z) {
               Waypoints.get().remove(waypoint);
               return;
            }
         }
      }
   }
}
