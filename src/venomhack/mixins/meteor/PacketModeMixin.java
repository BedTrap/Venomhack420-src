package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightMode;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes.Packet;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class_5911;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.class_2849;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(
   value = {Packet.class},
   remap = false
)
public class PacketModeMixin extends ElytraFlightMode {
   @Shadow
   @Final
   private Vec3d vec3d;

   public PacketModeMixin(ElytraFlightModes type) {
      super(type);
   }

   @Overwrite(
      remap = false
   )
   public void onTick() {
      super.onTick();
      if (this.mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA
         && !((double)this.mc.player.fallDistance <= 0.2)
         && !this.mc.options.sneakKey.isPressed()) {
         if (this.mc.options.forwardKey.isPressed()) {
            this.vec3d.add(0.0, 0.0, this.elytraFly.horizontalSpeed.get() * 0.05);
            this.vec3d.rotateY(-((float)Math.toRadians((double)this.mc.player.getYaw())));
         } else if (this.mc.options.backKey.isPressed()) {
            this.vec3d.add(0.0, 0.0, this.elytraFly.horizontalSpeed.get() * 0.05);
            this.vec3d.rotateY((float)Math.toRadians((double)this.mc.player.getYaw()));
         }

         if (this.mc.options.jumpKey.isPressed()) {
            this.vec3d.add(0.0, this.elytraFly.verticalSpeed.get() * 0.05, 0.0);
         } else if (!this.mc.options.jumpKey.isPressed()) {
            this.vec3d.add(0.0, -this.elytraFly.verticalSpeed.get() * 0.05, 0.0);
         }

         this.mc.player.setVelocity(this.vec3d);
         this.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this.mc.player, class_2849.START_FALL_FLYING));
         this.mc.player.networkHandler.sendPacket(new class_5911(true));
      }
   }

   @Overwrite(
      remap = false
   )
   public void onPlayerMove() {
      this.mc.player.getAbilities().flying = true;
      this.mc.player.getAbilities().setFlySpeed(((Double)this.elytraFly.horizontalSpeed.get()).floatValue() * 0.0025F);
   }
}
