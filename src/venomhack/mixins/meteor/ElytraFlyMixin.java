package venomhack.mixins.meteor;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightMode;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes.Packet;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes.Pitch40;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes.Vanilla;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.mixinInterface.IElytraFly;

@Mixin(
   value = {ElytraFly.class},
   remap = false
)
public class ElytraFlyMixin implements IElytraFly {
   @Shadow(
      remap = false
   )
   @Final
   private SettingGroup sgGeneral;
   @Shadow(
      remap = false
   )
   @Final
   public Setting<ElytraFlightModes> flightMode;
   @Shadow(
      remap = false
   )
   private ElytraFlightMode currentMode;
   @Shadow(
      remap = false
   )
   @Final
   public Setting<Boolean> autoPilot;
   @Unique
   private Setting<Boolean> phoenix = null;
   @Unique
   private Setting<Boolean> tpsSync = null;
   @Unique
   private Setting<Boolean> packetRoof = null;
   @Unique
   private Setting<Double> roofSpeed = null;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")},
      remap = false
   )
   private void onInit(CallbackInfo ci) {
      this.phoenix = this.sgGeneral
         .add(
            ((Builder)((Builder)((Builder)((Builder)new Builder().name("sync-vertical")).description("For PA. Reduces your horizontal speed while flying up."))
                     .defaultValue(false))
                  .visible(() -> this.flightMode.get() != ElytraFlightModes.Pitch40))
               .build()
         );
      this.tpsSync = this.sgGeneral
         .add(
            ((Builder)((Builder)((Builder)((Builder)new Builder().name("tps-sync")).description("Adjusts ur flying speed to the tps.")).defaultValue(false))
                  .visible(() -> this.flightMode.get() != ElytraFlightModes.Pitch40))
               .build()
         );
      this.packetRoof = this.sgGeneral
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("packet-mode-on-roof"))
                     .description("Automatically sets the mode to packet when flying on the roof to save durability."))
                  .defaultValue(false))
               .build()
         );
      this.roofSpeed = this.sgGeneral
         .add(
            ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder(
                           
                        )
                        .name("roof-speed"))
                     .description("How fast you go on the Nether roof in mps."))
                  .defaultValue(16.0)
                  .sliderMax(50.0)
                  .visible(() -> this.flightMode.get() != ElytraFlightModes.Pitch40))
               .build()
         );
   }

   @Overwrite(
      remap = false
   )
   private void onModeChanged(ElytraFlightModes mode) {
      switch(mode) {
         case Vanilla:
            this.currentMode = new Vanilla();
            if (MeteorClient.mc.player != null) {
               MeteorClient.mc.player.getAbilities().flying = false;
            }
            break;
         case Packet:
            this.currentMode = new Packet();
            break;
         case Pitch40:
            this.currentMode = new Pitch40();
            this.autoPilot.set(false);
            if (MeteorClient.mc.player != null) {
               MeteorClient.mc.player.getAbilities().flying = false;
            }
      }
   }

   @Inject(
      method = {"onPlayerMove"},
      at = {@At("HEAD")},
      remap = false
   )
   private void onPlayerMove(PlayerMoveEvent event, CallbackInfo ci) {
      if (this.packetRoof.get() && PlayerUtils.getDimension() == Dimension.Nether) {
         if (MeteorClient.mc.player.getY() > 128.0) {
            if (this.flightMode.get() != ElytraFlightModes.Packet) {
               this.flightMode.set(ElytraFlightModes.Packet);
            }
         } else if (this.flightMode.get() == ElytraFlightModes.Packet) {
            this.flightMode.set(ElytraFlightModes.Vanilla);
         }
      }
   }

   @Unique
   @Override
   public boolean getPhoenix() {
      return this.phoenix.get();
   }

   @Unique
   @Override
   public boolean getTpsSync() {
      return this.tpsSync.get();
   }

   @Unique
   @Override
   public double getRoofSpeed() {
      return this.roofSpeed.get();
   }
}
