package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Step;
import meteordevelopment.meteorclient.systems.modules.movement.Step.ActiveWhen;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.util.math.Box;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class_2829;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.movement.Anchor;

@Mixin(
   value = {Step.class},
   remap = false
)
public abstract class StepMixin {
   @Shadow(
      remap = false
   )
   @Final
   private SettingGroup sgGeneral;
   @Shadow(
      remap = false
   )
   @Final
   public Setting<Double> height;
   @Shadow(
      remap = false
   )
   @Final
   private Setting<ActiveWhen> activeWhen;
   @Shadow(
      remap = false
   )
   @Final
   private Setting<Boolean> safeStep;
   @Shadow(
      remap = false
   )
   @Final
   private Setting<Integer> stepHealth;
   @Unique
   private Setting<Boolean> ncp = null;

   @Shadow(
      remap = false
   )
   public abstract void onDeactivate();

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")},
      remap = false
   )
   private void onInit(CallbackInfo ci) {
      this.ncp = this.sgGeneral
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("ncp-mode")).description("Attempts to bypass strict no-cheat-plus servers.")).defaultValue(false))
               .build()
         );
   }

   @Inject(
      method = {"onTick"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   private void onTick(Post event, CallbackInfo ci) {
      if (((Anchor)Modules.get().get(Anchor.class)).cancelStep()) {
         this.onDeactivate();
         ci.cancel();
      } else {
         if (this.ncp.get()) {
            this.doNcpStep();
            ci.cancel();
         }
      }
   }

   @Unique
   private void doNcpStep() {
      MeteorClient.mc.player.stepHeight = 0.6F;
      switch((ActiveWhen)this.activeWhen.get()) {
         case Sneaking:
            if (!MeteorClient.mc.player.isSneaking()) {
               return;
            }
            break;
         case NotSneaking:
            if (MeteorClient.mc.player.isSneaking()) {
               return;
            }
      }

      if (!this.safeStep.get()
         || !(PlayerUtils.getTotalHealth() <= (double)((Integer)this.stepHealth.get()).intValue())
            && !(PlayerUtils.getTotalHealth() - PlayerUtils.possibleHealthReductions() <= (double)((Integer)this.stepHealth.get()).intValue())) {
         MeteorClient.mc.player.stepHeight = ((Double)this.height.get()).floatValue();
         if (MeteorClient.mc.player.horizontalCollision
            && MeteorClient.mc.player.isOnGround()
            && !MeteorClient.mc.player.isHoldingOntoLadder()
            && !MeteorClient.mc.options.jumpKey.isPressed()
            && MeteorClient.mc.player.fallDistance == 0.0F
            && (MeteorClient.mc.player.forwardSpeed != 0.0F || MeteorClient.mc.player.sidewaysSpeed != 0.0F)) {
            for(double i = 1.0; i <= this.height.get(); i += 0.5) {
               Box box = MeteorClient.mc.player.getBoundingBox().offset(0.0, i, 0.0);
               if (MeteorClient.mc.world.isSpaceEmpty(box.expand(0.05, 0.0, 0.0))
                  && MeteorClient.mc.world.isSpaceEmpty(box.expand(0.0, 0.0, 0.05))) {
                  if (i == 1.0 && this.height.get() == 1.0) {
                     this.sendPacket(0.41999998688698);
                     this.sendPacket(0.7531999805212);
                     MeteorClient.mc
                        .player
                        .updatePosition(
                           MeteorClient.mc.player.getX(),
                           MeteorClient.mc.player.getY() + 1.0,
                           MeteorClient.mc.player.getZ()
                        );
                  }

                  if (i == 1.5) {
                     this.sendPacket(0.41999998688698);
                     this.sendPacket(0.7531999805212);
                     this.sendPacket(1.00133597911214);
                     this.sendPacket(1.16610926093821);
                     this.sendPacket(1.24918707874468);
                     this.sendPacket(1.1707870772188);
                     MeteorClient.mc
                        .player
                        .updatePosition(
                           MeteorClient.mc.player.getX(),
                           MeteorClient.mc.player.getY() + 1.0,
                           MeteorClient.mc.player.getZ()
                        );
                  }

                  if (i == 2.0) {
                     this.sendPacket(0.42);
                     this.sendPacket(0.78);
                     this.sendPacket(0.63);
                     this.sendPacket(0.51);
                     this.sendPacket(0.9);
                     this.sendPacket(1.21);
                     this.sendPacket(1.45);
                     this.sendPacket(1.43);
                     MeteorClient.mc
                        .player
                        .updatePosition(
                           MeteorClient.mc.player.getX(),
                           MeteorClient.mc.player.getY() + 2.0,
                           MeteorClient.mc.player.getZ()
                        );
                  }
                  break;
               }
            }
         }
      }
   }

   @Unique
   private void sendPacket(double y) {
      MeteorClient.mc
         .player
         .networkHandler
         .sendPacket(
            new class_2829(
               MeteorClient.mc.player.getX(),
               MeteorClient.mc.player.getY() + y,
               MeteorClient.mc.player.getZ(),
               MeteorClient.mc.player.isOnGround()
            )
         );
   }
}
