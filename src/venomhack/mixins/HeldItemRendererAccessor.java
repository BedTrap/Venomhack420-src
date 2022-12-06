package venomhack.mixins;

import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({HeldItemRenderer.class})
public interface HeldItemRendererAccessor {
   @Accessor("equipProgressMainHand")
   void setEquipProgressMainhand(float var1);

   @Accessor("prevEquipProgressMainHand")
   void setprevEquipProgressMainHand(float var1);

   @Accessor("equipProgressOffHand")
   void setEquipProgressOffhand(float var1);

   @Accessor("prevEquipProgressOffHand")
   void setprevEquipProgressOffHand(float var1);
}
