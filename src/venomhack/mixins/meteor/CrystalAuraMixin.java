package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import venomhack.mixinInterface.IModule;

@Mixin(
   value = {CrystalAura.class},
   remap = false
)
public class CrystalAuraMixin implements IModule {
   @Shadow
   private PlayerEntity bestTarget;
   @Shadow
   private int bestTargetTimer;

   @Unique
   @Override
   public PlayerEntity getTarget() {
      return this.bestTargetTimer <= 0 ? null : this.bestTarget;
   }
}
