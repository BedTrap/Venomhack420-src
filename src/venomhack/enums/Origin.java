package venomhack.enums;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.util.math.Vec3d;

public enum Origin {
   VANILLA("Vanilla"),
   NCP("No Cheat Plus");

   private final String title;

   private Origin(String title) {
      this.title = title;
   }

   @Override
   public String toString() {
      return this.title;
   }

   public Vec3d getOrigin(Vec3d pos) {
      return this == VANILLA ? pos : pos.add(0.0, (double)MeteorClient.mc.player.getEyeHeight(MeteorClient.mc.player.getPose()), 0.0);
   }
}
