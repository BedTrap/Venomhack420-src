package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightMode;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import venomhack.mixinInterface.IElytraFly;

@Mixin(
   value = {ElytraFlightMode.class},
   remap = false
)
public class ElytraFlightModeMixin {
   @Shadow(
      remap = false
   )
   @Final
   protected ElytraFly elytraFly;
   @Shadow(
      remap = false
   )
   protected double velX;
   @Shadow(
      remap = false
   )
   protected double velZ;
   @Shadow(
      remap = false
   )
   protected double velY;
   @Shadow(
      remap = false
   )
   protected Vec3d forward;
   @Shadow(
      remap = false
   )
   protected Vec3d right;

   @Overwrite(
      remap = false
   )
   public void handleHorizontalSpeed(PlayerMoveEvent event) {
      boolean a = false;
      boolean b = false;
      double tickFactor = ((IElytraFly)this.elytraFly).getTpsSync() ? (double)TickRate.INSTANCE.getTickRate() * 0.05 : 1.0;
      double hSpeed = PlayerUtils.getDimension() == Dimension.Nether && MeteorClient.mc.player.getY() > 128.0
         ? ((IElytraFly)this.elytraFly).getRoofSpeed()
         : this.elytraFly.horizontalSpeed.get();
      hSpeed *= tickFactor;
      if (MeteorClient.mc.options.forwardKey.isPressed()) {
         this.velX += this.transform(this.forward.x) * hSpeed;
         this.velZ += this.transform(this.forward.z) * hSpeed;
         a = true;
      } else if (MeteorClient.mc.options.backKey.isPressed()) {
         this.velX -= this.transform(this.forward.x) * hSpeed;
         this.velZ -= this.transform(this.forward.z) * hSpeed;
         a = true;
      }

      if (MeteorClient.mc.options.rightKey.isPressed()) {
         this.velX += this.transform(this.right.x) * hSpeed;
         this.velZ += this.transform(this.right.z) * hSpeed;
         b = true;
      } else if (MeteorClient.mc.options.leftKey.isPressed()) {
         this.velX -= this.transform(this.right.x) * hSpeed;
         this.velZ -= this.transform(this.right.z) * hSpeed;
         b = true;
      }

      if (a && b) {
         double diagonal = 0.70710678118;
         this.velX *= diagonal;
         this.velZ *= diagonal;
      }

      double vSpeed = this.elytraFly.verticalSpeed.get() * 0.05;
      if (MeteorClient.mc.options.jumpKey.isPressed() && !MeteorClient.mc.options.sneakKey.isPressed()) {
         vSpeed = PlayerUtils.getDimension() == Dimension.Nether && MeteorClient.mc.player.getY() > 128.0
            ? ((IElytraFly)this.elytraFly).getRoofSpeed()
            : this.elytraFly.verticalSpeed.get();
         vSpeed *= tickFactor * 0.05;
         this.velY += vSpeed;
         if (((IElytraFly)this.elytraFly).getPhoenix()) {
            Vec3d travelVec = MeteorClient.mc.player.getPos().add(this.velX * 20.0, this.velY * 20.0, this.velZ * 20.0);
            double factor = hSpeed / MeteorClient.mc.player.getPos().distanceTo(travelVec);
            if (factor < 1.0) {
               this.velX *= factor;
               this.velY *= factor;
               this.velZ *= factor;
            }
         }
      } else if (MeteorClient.mc.options.sneakKey.isPressed() && !MeteorClient.mc.options.jumpKey.isPressed()) {
         this.velY -= vSpeed;
      }
   }

   @Unique
   private double transform(double momentum) {
      return MathHelper.clamp(momentum, -1.0, 1.0) * 0.5;
   }

   @Overwrite(
      remap = false
   )
   public void handleVerticalSpeed(PlayerMoveEvent event) {
   }
}
