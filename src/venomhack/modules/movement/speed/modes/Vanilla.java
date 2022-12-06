package venomhack.modules.movement.speed.modes;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Anchor;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;
import venomhack.modules.movement.speed.Speed;
import venomhack.modules.movement.speed.SpeedMode;

public class Vanilla extends SpeedMode {
   public Vanilla() {
      super(Speed.SpeedModes.VANILLA);
   }

   @Override
   public void onMove(PlayerMoveEvent event) {
      Vec3d vel = PlayerUtils.getDimension() == Dimension.Nether && this.mc.player.getBlockPos().getY() >= 127
         ? PlayerUtils.getHorizontalVelocity(this.settings.vanillaRoofSpeed.get())
         : PlayerUtils.getHorizontalVelocity(this.settings.vanillaSpeed.get());
      double velX = vel.getX();
      double velZ = vel.getZ();
      if (this.settings.vanillaTps.get()) {
         velX *= (double)(TickRate.INSTANCE.getTickRate() / 20.0F);
         velZ *= (double)(TickRate.INSTANCE.getTickRate() / 20.0F);
      }

      if (this.mc.player.hasStatusEffect(StatusEffects.SPEED)) {
         double value = (double)(this.mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1) * 0.205;
         velX += velX * value;
         velZ += velZ * value;
      }

      Anchor anchor = (Anchor)Modules.get().get(Anchor.class);
      if (anchor.isActive() && anchor.controlMovement) {
         velX = anchor.deltaX;
         velZ = anchor.deltaZ;
      }

      ((IVec3d)event.movement).set(velX, event.movement.y, velZ);
   }
}
