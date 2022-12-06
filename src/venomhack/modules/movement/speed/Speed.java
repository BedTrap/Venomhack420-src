package venomhack.modules.movement.speed;

import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;
import venomhack.modules.movement.speed.modes.Strafe;
import venomhack.modules.movement.speed.modes.Vanilla;

public class Speed extends ModuleHelper {
   public final Setting<Speed.SpeedModes> speedMode = this.setting(
      "mode",
      "The method of applying speed.",
      Speed.SpeedModes.VANILLA,
      null,
      this::onSpeedModeChanged,
      speedModesSetting -> this.onSpeedModeChanged((Speed.SpeedModes)speedModesSetting.get())
   );
   public final Setting<Double> timer = this.setting("timer", "Timer override.", Double.valueOf(1.0));
   public final Setting<Boolean> inLiquids = this.setting("in-liquids", "Uses speed when in lava or water.", Boolean.valueOf(false));
   public final Setting<Boolean> whenSneaking = this.setting("when-sneaking", "Uses speed when sneaking.", Boolean.valueOf(false));
   public final Setting<Double> vanillaSpeed = this.setting(
      "vanilla-speed",
      "The speed in blocks per second.",
      Double.valueOf(24.0),
      this.sgGeneral,
      () -> this.speedMode.get() == Speed.SpeedModes.VANILLA,
      0.0,
      18.0
   );
   public final Setting<Double> vanillaRoofSpeed = this.setting(
      "roof-speed",
      "The speed in blocks per second for the nether roof.",
      Double.valueOf(16.0),
      this.sgGeneral,
      () -> this.speedMode.get() == Speed.SpeedModes.VANILLA,
      0.0,
      16.0
   );
   public final Setting<Boolean> vanillaTps = this.setting(
      "apply-tps", "Whether to reduce the speed when the tps drops or not.", Boolean.valueOf(false), () -> this.speedMode.get() == Speed.SpeedModes.VANILLA
   );
   public final Setting<Boolean> vanillaOnGround = this.setting(
      "only-on-ground", "Uses speed only when standing on a block.", Boolean.valueOf(false), () -> this.speedMode.get() == Speed.SpeedModes.VANILLA
   );
   public final Setting<Double> ncpSpeed = this.setting(
      "air-speed-multiplier",
      "Multiplies by how fast you go while in air.",
      Double.valueOf(1.8),
      this.sgGeneral,
      () -> this.speedMode.get() == Speed.SpeedModes.STRAFE,
      0.0,
      3.0
   );
   public final Setting<Double> ncpGroundSpeedMultiplier = this.setting(
      "ground-speed-multiplier",
      "Multiplies by how fast you go while on ground.",
      Double.valueOf(1.18),
      this.sgGeneral,
      () -> this.speedMode.get() == Speed.SpeedModes.STRAFE,
      0.0,
      1.5
   );
   public final Setting<Boolean> ncpSpeedLimit = this.setting(
      "speed-limit",
      "Limits your speed on servers with very strict anti cheats.",
      Boolean.valueOf(false),
      () -> this.speedMode.get() == Speed.SpeedModes.STRAFE
   );
   public final Setting<Boolean> ncpAutoJump = this.setting(
      "auto-jump", "Whether to automatically jump when using strafe.", Boolean.valueOf(true), () -> this.speedMode.get() == Speed.SpeedModes.STRAFE
   );
   public final Setting<Double> ncpJumpHeight = this.setting(
      "auto-jump-height",
      "Height for auto jumps.",
      Double.valueOf(0.40123128),
      this.sgGeneral,
      () -> this.speedMode.get() == Speed.SpeedModes.STRAFE && this.ncpAutoJump.get(),
      0.0,
      1.0
   );
   public final Setting<Speed.RubberBand> rubMode = this.setting("rubberband-behavior", "What to do when rubberbanding.", Speed.RubberBand.DISABLE);
   public final Setting<Integer> rubPauseTime = this.setting(
      "pause-amount",
      "For how long to pause in ticks when rubberbanding.",
      Integer.valueOf(5),
      this.sgGeneral,
      () -> this.rubMode.get() == Speed.RubberBand.PAUSE,
      20.0
   );
   private SpeedMode currentMode;

   public Speed() {
      super(Venomhack420.CATEGORY, "speed-vh", "Modifies your speed when moving.");
      this.onSpeedModeChanged((Speed.SpeedModes)this.speedMode.get());
   }

   public void onActivate() {
      this.currentMode.onActivate();
   }

   public void onDeactivate() {
      ((Timer)Modules.get().get(Timer.class)).setOverride(1.0);
      this.currentMode.onDeactivate();
   }

   @EventHandler
   private void onPlayerMove(PlayerMoveEvent event) {
      if (event.type == MovementType.SELF
         && !this.mc.player.isFallFlying()
         && !this.mc.player.isClimbing()
         && this.mc.player.getVehicle() == null) {
         if (this.whenSneaking.get() || !this.mc.player.isSneaking()) {
            if (!this.vanillaOnGround.get() || this.mc.player.isOnGround() || this.speedMode.get() != Speed.SpeedModes.VANILLA) {
               if (this.inLiquids.get() || !this.mc.player.isTouchingWater() && !this.mc.player.isInLava()) {
                  this.currentMode.onMove(event);
               }
            }
         }
      }
   }

   @EventHandler
   private void onPreTick(Pre event) {
      if (!this.mc.player.isFallFlying() && !this.mc.player.isClimbing() && this.mc.player.getVehicle() == null) {
         if (this.whenSneaking.get() || !this.mc.player.isSneaking()) {
            if (!this.vanillaOnGround.get() || this.mc.player.isOnGround() || this.speedMode.get() != Speed.SpeedModes.VANILLA) {
               if (this.inLiquids.get() || !this.mc.player.isTouchingWater() && !this.mc.player.isInLava()) {
                  this.currentMode.onTick();
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacketReceive(Receive event) {
      if (event.packet instanceof PlayerPositionLookS2CPacket) {
         this.currentMode.onRubberband();
      }
   }

   @EventHandler
   private void onJumpVelocityMultiplier(JumpVelocityMultiplierEvent event) {
      if (this.speedMode.get() != Speed.SpeedModes.VANILLA) {
         this.currentMode.onJumpHeight(event);
      }
   }

   private void onSpeedModeChanged(Speed.SpeedModes mode) {
      switch(mode) {
         case VANILLA:
            this.currentMode = new Vanilla();
            break;
         case STRAFE:
            this.currentMode = new Strafe();
      }
   }

   public String getInfoString() {
      return this.currentMode.getHudString();
   }

   public static enum RubberBand {
      DISABLE("Disable"),
      SLOWDOWN("Slow Down"),
      PAUSE("Pause"),
      NONE("None");

      private final String title;

      private RubberBand(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }

   public static enum SpeedModes {
      STRAFE("NCP Strafe"),
      VANILLA("Vanilla Speed");

      private final String title;

      private SpeedModes(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
