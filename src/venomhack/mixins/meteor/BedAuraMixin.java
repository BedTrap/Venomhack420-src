package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.systems.modules.combat.BedAura;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import venomhack.mixinInterface.IModule;

@Mixin(
   value = {BedAura.class},
   remap = false
)
public class BedAuraMixin implements IModule {
   @Shadow(
      remap = false
   )
   private PlayerEntity target;

   @Unique
   @Override
   public PlayerEntity getTarget() {
      return this.target;
   }
}
