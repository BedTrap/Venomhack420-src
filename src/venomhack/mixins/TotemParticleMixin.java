package venomhack.mixins;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.particle.AnimatedParticle;
import net.minecraft.client.particle.TotemParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.render.BetterPops;

@Mixin({TotemParticle.class})
public abstract class TotemParticleMixin extends AnimatedParticle {
   protected TotemParticleMixin(ClientWorld world, double x, double y, double z, SpriteProvider spriteProvider, float upwardsAcceleration) {
      super(world, x, y, z, spriteProvider, upwardsAcceleration);
   }

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void onConfettiConstructor(
      ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider, CallbackInfo ci
   ) {
      BetterPops betterPops = (BetterPops)Modules.get().get(BetterPops.class);
      TotemParticle totemParticle = (TotemParticle)this;
      if (betterPops.isActive()) {
         boolean isSelf = betterPops.isSelf;
         Vec3d colorOne = betterPops.getDoubleVectorColor(isSelf ? betterPops.colorOneSelf : betterPops.colorOneOther);
         Vec3d colorTwo = betterPops.getDoubleVectorColor(isSelf ? betterPops.colorTwoSelf : betterPops.colorTwoOther);
         Vec3d colorThree = betterPops.getDoubleVectorColor(isSelf ? betterPops.colorThreeSelf : betterPops.colorThreeOther);
         Vec3d colorFour = betterPops.getDoubleVectorColor(isSelf ? betterPops.colorFourSelf : betterPops.colorFourOther);
         float sizeOne = isSelf ? ((Double)betterPops.sizeOneSelf.get()).floatValue() : ((Double)betterPops.sizeOneOther.get()).floatValue();
         float sizeTwo = isSelf ? ((Double)betterPops.sizeTwoSelf.get()).floatValue() : ((Double)betterPops.sizeTwoOther.get()).floatValue();
         float sizeThree = isSelf ? ((Double)betterPops.sizeThreeSelf.get()).floatValue() : ((Double)betterPops.sizeThreeOther.get()).floatValue();
         float sizeFour = isSelf ? ((Double)betterPops.sizeFourSelf.get()).floatValue() : ((Double)betterPops.sizeFourOther.get()).floatValue();
         int randInt = this.random.nextInt(4);
         if (randInt == 0) {
            if (betterPops.shouldChangeColor(isSelf)) {
               totemParticle.setColor((float)colorOne.x, (float)colorOne.y, (float)colorOne.z);
            }

            if (betterPops.shouldChangeSize(isSelf)) {
               totemParticle.scale(sizeOne);
            }
         } else if (randInt == 1) {
            if (betterPops.shouldChangeColor(isSelf)) {
               totemParticle.setColor((float)colorTwo.x, (float)colorTwo.y, (float)colorTwo.z);
            }

            if (betterPops.shouldChangeSize(isSelf)) {
               totemParticle.scale(sizeTwo);
            }
         } else if (randInt == 2) {
            if (betterPops.shouldChangeColor(isSelf)) {
               totemParticle.setColor((float)colorThree.x, (float)colorThree.y, (float)colorThree.z);
            }

            if (betterPops.shouldChangeSize(isSelf)) {
               totemParticle.scale(sizeThree);
            }
         } else {
            if (betterPops.shouldChangeColor(isSelf)) {
               totemParticle.setColor((float)colorFour.x, (float)colorFour.y, (float)colorFour.z);
            }

            if (betterPops.shouldChangeSize(isSelf)) {
               totemParticle.scale(sizeFour);
            }
         }
      }
   }
}
