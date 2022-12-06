package venomhack.modules.render;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.Vec3d;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class TanukiOutline extends ModuleHelper {
   public final Setting<SettingColor> lineColor = this.setting("color", "The outline's color.", 255, 255, 255, 255);

   public TanukiOutline() {
      super(Venomhack420.CATEGORY, "tanuki-outline", "Block indicator from Tanuki. Credits to Walaryne.");
   }

   public Vec3d getColors() {
      return this.getDoubleVectorColor(this.lineColor);
   }

   public double getAlpha() {
      return (double)((SettingColor)this.lineColor.get()).a / 255.0;
   }

   private Vec3d getDoubleVectorColor(Setting<SettingColor> colorSetting) {
      return new Vec3d(
         (double)((SettingColor)colorSetting.get()).r / 255.0,
         (double)((SettingColor)colorSetting.get()).g / 255.0,
         (double)((SettingColor)colorSetting.get()).b / 255.0
      );
   }
}
