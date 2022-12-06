package venomhack.modules.movement;

import meteordevelopment.meteorclient.settings.Setting;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class Moses extends ModuleHelper {
   public final Setting<Boolean> lava = this.setting("lava", "Applies to lava too.", Boolean.valueOf(false));

   public Moses() {
      super(Venomhack420.CATEGORY, "moses", "Lets you walk through water as if it was air.");
   }
}
