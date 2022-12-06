package venomhack.modules.render;

import meteordevelopment.meteorclient.settings.Setting;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class DroppedItemsView extends ModuleHelper {
   public final Setting<Double> rotationXDropped = this.setting("rotation-x-dropped", "The X rotation of dropped items.", Double.valueOf(0.0), 0.0, 360.0);
   public final Setting<Double> rotationYDropped = this.setting("rotation-y-dropped", "The Y rotation of dropped items.", Double.valueOf(0.0), 0.0, 360.0);
   public final Setting<Double> rotationZDropped = this.setting("rotation-z-dropped", "The Z rotation of dropped items.", Double.valueOf(0.0), 0.0, 360.0);
   public final Setting<Double> scaleXDropped = this.setting("scale-x-dropped", "The X scale of dropped items.", Double.valueOf(1.0), 0.0, 5.0);
   public final Setting<Double> scaleYDropped = this.setting("scale-y-dropped", "The Y scale of dropped items.", Double.valueOf(1.0), 0.0, 5.0);
   public final Setting<Double> scaleZDropped = this.setting("scale-z-dropped", "The Z scale of dropped items.", Double.valueOf(1.0), 0.0, 5.0);
   public final Setting<Double> rotationXBlocksDropped = this.setting(
      "rotation-x-dropped-blocks", "The X rotation of dropped blocks.", Double.valueOf(0.0), 0.0, 360.0
   );
   public final Setting<Double> rotationYBlocksDropped = this.setting(
      "rotation-y-dropped-blocks", "The Y rotation of dropped blocks.", Double.valueOf(0.0), 0.0, 360.0
   );
   public final Setting<Double> rotationZBlocksDropped = this.setting(
      "rotation-z-dropped-blocks", "The Z rotation of dropped blocks.", Double.valueOf(0.0), 0.0, 360.0
   );
   public final Setting<Double> scaleXYZBlocksDropped = this.setting("scale-of-dropped-blocks", "The scale of dropped blocks.", Double.valueOf(1.0), 0.0, 5.0);

   public DroppedItemsView() {
      super(Venomhack420.CATEGORY, "dropped-items-view", "Alters the way dropped items and blocks render.");
   }
}
