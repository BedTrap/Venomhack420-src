package venomhack.modules.movement.speed.modes;

import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Anchor;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.misc.Vec2;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.class_2849;
import venomhack.modules.movement.speed.Speed;
import venomhack.modules.movement.speed.SpeedMode;

public class Strafe extends SpeedMode {
   private long timer = 0L;

   public Strafe() {
      super(Speed.SpeedModes.STRAFE);
   }

   @Override
   public void onMove(PlayerMoveEvent event) {
      ((Timer)Modules.get().get(Timer.class)).setOverride(PlayerUtils.isMoving() && this.stage < 5 ? this.settings.timer.get() : 1.0);
      switch(this.stage) {
         case 0:
            if (PlayerUtils.isMoving()) {
               ++this.stage;
               this.speed = this.settings.ncpGroundSpeedMultiplier.get() * this.getDefaultSpeed() - 0.01;
            }
         case 1:
            if (this.settings.ncpAutoJump.get() && PlayerUtils.isMoving() && this.mc.player.isOnGround()) {
               ((IVec3d)event.movement).setY(this.getHop(this.settings.ncpJumpHeight.get()));
               this.speed *= this.settings.ncpSpeed.get();
               ++this.stage;
            }
            break;
         case 2:
            this.speed = this.distance - 0.76 * (this.distance - this.getDefaultSpeed());
            ++this.stage;
            break;
         case 3:
            if (!this.mc.world.isSpaceEmpty(this.mc.player.getBoundingBox().offset(0.0, this.mc.player.getVelocity().y, 0.0))
               || this.mc.player.verticalCollision && this.stage > 0) {
               this.stage = 0;
            }

            this.speed = this.distance - this.distance / 159.0;
            break;
         case 4:
            this.stage = 2;
            break;
         default:
            --this.stage;
            return;
      }

      this.speed = Math.max(this.speed, this.getDefaultSpeed());
      if (this.settings.ncpSpeedLimit.get()) {
         if (System.currentTimeMillis() - this.timer > 2500L) {
            this.timer = System.currentTimeMillis();
         }

         this.speed = Math.min(this.speed, System.currentTimeMillis() - this.timer > 1250L ? 0.44 : 0.43);
      }

      Vec2 change = this.transformStrafe(this.speed);
      double velX = change.x;
      double velZ = change.y;
      Anchor anchor = (Anchor)Modules.get().get(Anchor.class);
      venomhack.modules.movement.Anchor anchor2 = (venomhack.modules.movement.Anchor)Modules.get().get(venomhack.modules.movement.Anchor.class);
      if (anchor.isActive() && anchor.controlMovement) {
         velX = anchor.deltaX;
         velZ = anchor.deltaZ;
      } else if (anchor2.isActive() && anchor2.controlMovement) {
         return;
      }

      ((IVec3d)event.movement).setXZ(velX, velZ);
   }

   @Override
   public void onJumpHeight(JumpVelocityMultiplierEvent event) {
      this.speed *= this.settings.ncpSpeed.get();
      this.stage = 4;
   }

   @Override
   public void onRubberband() {
      if (this.settings.rubMode.get() == Speed.RubberBand.DISABLE) {
         this.settings.warning("Rubberband detected! Disabling...", new Object[0]);
         this.settings.toggle();
      } else if (this.settings.rubMode.get() == Speed.RubberBand.SLOWDOWN) {
         this.speed = this.distance - 0.76 * (this.distance - this.getDefaultSpeed());
         this.stage = 5;
         this.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this.mc.player, class_2849.PRESS_SHIFT_KEY));
         this.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this.mc.player, class_2849.RELEASE_SHIFT_KEY));
      } else if (this.settings.rubMode.get() == Speed.RubberBand.PAUSE) {
         this.reset();
         this.stage = this.settings.rubPauseTime.get();
         this.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this.mc.player, class_2849.PRESS_SHIFT_KEY));
         this.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this.mc.player, class_2849.RELEASE_SHIFT_KEY));
      }
   }

   private Vec2 transformStrafe(double speed) {
      float forward = this.mc.player.input.movementForward;
      float side = this.mc.player.input.movementSideways;
      float yaw = this.mc.player.prevYaw + (this.mc.player.getYaw() - this.mc.player.prevYaw) * this.mc.getTickDelta();
      if (forward == 0.0F && side == 0.0F) {
         return new Vec2(0.0, 0.0);
      } else {
         if (forward != 0.0F) {
            if (side >= 1.0F) {
               yaw += (float)(forward > 0.0F ? -45 : 45);
               side = 0.0F;
            } else if (side <= -1.0F) {
               yaw += (float)(forward > 0.0F ? 45 : -45);
               side = 0.0F;
            }

            if (forward > 0.0F) {
               forward = 1.0F;
            } else if (forward < 0.0F) {
               forward = -1.0F;
            }
         }

         double mx = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
         double mz = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
         double velX = (double)forward * speed * mx + (double)side * speed * mz;
         double velZ = (double)forward * speed * mz - (double)side * speed * mx;
         return new Vec2(velX, velZ);
      }
   }

   @Override
   public void onTick() {
      this.distance = Math.sqrt(
         (this.mc.player.getX() - this.mc.player.prevX) * (this.mc.player.getX() - this.mc.player.prevX)
            + (this.mc.player.getZ() - this.mc.player.prevZ) * (this.mc.player.getZ() - this.mc.player.prevZ)
      );
   }
}
