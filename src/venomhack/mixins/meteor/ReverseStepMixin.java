package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.LongJump;
import meteordevelopment.meteorclient.systems.modules.movement.ReverseStep;
import meteordevelopment.meteorclient.systems.modules.movement.speed.Speed;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedModes;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.movement.PacketFly;

@Mixin(
   value = {ReverseStep.class},
   remap = false
)
public class ReverseStepMixin {
   @Shadow(
      remap = false
   )
   @Final
   private SettingGroup sgGeneral;
   @Shadow(
      remap = false
   )
   @Final
   private Setting<Double> fallSpeed;
   @Shadow(
      remap = false
   )
   @Final
   private Setting<Double> fallDistance;
   @Unique
   private Setting<Boolean> ncp = null;
   @Unique
   private boolean resetTimer = false;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")},
      remap = false
   )
   private void onInit(CallbackInfo ci) {
      this.ncp = this.sgGeneral
         .add(((Builder)((Builder)((Builder)new Builder().name("bypass-mode")).description("Attempts to bypass.")).defaultValue(false)).build());
   }

   @Inject(
      method = {"onTick"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   private void onTick(Post event, CallbackInfo ci) {
      if (this.ncp.get()) {
         Modules mods = Modules.get();
         if (this.works()
            && (!mods.isActive(Speed.class) || ((Speed)mods.get(Speed.class)).speedMode.get() == SpeedModes.Vanilla)
            && !mods.isActive(LongJump.class)
            && !mods.isActive(PacketFly.class)) {
            this.resetTimer = false;
            ((Timer)mods.get(Timer.class)).setOverride(this.fallSpeed.get());
            Vec3d v = MeteorClient.mc.player.getVelocity();
            MeteorClient.mc.player.setVelocity(v.x / this.fallSpeed.get(), v.y, v.z / this.fallSpeed.get());
         } else if (!this.resetTimer) {
            this.resetTimer = true;
            ((Timer)mods.get(Timer.class)).setOverride(1.0);
         }

         ci.cancel();
      }
   }

   @Unique
   private boolean works() {
      if (MeteorClient.mc.player.isOnGround()) {
         return false;
      } else if (MeteorClient.mc.player.getVelocity().y > 0.0) {
         return false;
      } else if (MeteorClient.mc.player.isSubmergedInWater() || MeteorClient.mc.player.isInLava()) {
         return false;
      } else if (!MeteorClient.mc.player.isHoldingOntoLadder() && !MeteorClient.mc.player.isFallFlying()) {
         if (!MeteorClient.mc.options.jumpKey.isPressed()
            && (MeteorClient.mc.player.forwardSpeed != 0.0F || MeteorClient.mc.player.sidewaysSpeed != 0.0F)) {
            if (MeteorClient.mc.player.noClip || MeteorClient.mc.player.getAbilities().flying) {
               return false;
            } else if ((double)MeteorClient.mc.player.fallDistance > this.fallDistance.get()) {
               return false;
            } else {
               return MeteorClient.mc.world.getBlockState(MeteorClient.mc.player.getBlockPos()).getBlock() != Blocks.COBWEB;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
