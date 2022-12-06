package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.systems.modules.world.AirPlace;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {AirPlace.class},
   remap = false
)
public interface AirPlaceAccessor {
   @Accessor("hitResult")
   void setHitResult(HitResult var1);
}
