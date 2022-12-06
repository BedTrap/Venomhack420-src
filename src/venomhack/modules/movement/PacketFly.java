package venomhack.modules.movement;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket.class_2709;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class_2829;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;
import venomhack.utils.customObjects.Timer;

public class PacketFly extends ModuleHelper {
   private final SettingGroup sgBypass = this.group("Bypass");
   private final SettingGroup sgFactor = this.group("Factor");
   private final SettingGroup sgPhase = this.group("Phase");
   private final Setting<PacketFly.Type> type = this.setting("type", "What type of packet fly to perform.", PacketFly.Type.FAST);
   private final Setting<PacketFly.Mode> packetMode = this.setting("packet-mode", "What kind of packets to send.", PacketFly.Mode.UP);
   private final Setting<Double> speed = this.setting("speed", "Horizontal speed.", 1.0, this.sgGeneral, 0.1, 2.0, 1);
   private final Setting<PacketFly.AntiKick> antiKickMode = this.setting("anti-kick-mode", "Anti kick mode.", PacketFly.AntiKick.NORMAL);
   private final Setting<Double> boostTimer = this.setting("timer", "Timer overwrite.", 1.0, this.sgGeneral, 1.0, 5.0, 2);
   private final Setting<Boolean> timerMoving = this.setting(
      "only-when-moving", "Will not activate timer when not moving.", Boolean.valueOf(true), () -> this.boostTimer.get() != 1.0
   );
   private final Setting<Boolean> strict = this.setting("strict", "Strict mode.", Boolean.valueOf(true), this.sgBypass);
   private final Setting<Boolean> bounds = this.setting(
      "bounds", "Attempts to keep the sent packet's positions close to the player.", Boolean.valueOf(true), this.sgBypass
   );
   private final Setting<PacketFly.Limit> limit = this.setting("limit", "Limits packets probably.", PacketFly.Limit.NONE, this.sgBypass);
   private final Setting<Boolean> constrict = this.setting(
      "extra-packet", "Sends an additional packet of the current position.", Boolean.valueOf(false), this.sgBypass
   );
   private final Setting<Boolean> extraCt = this.setting(
      "teleport-confirm", "Will send an additional teleport confirm packet.", Boolean.valueOf(false), this.sgBypass
   );
   private final Setting<Boolean> jitter = this.setting("jitter", "Sets your velocity to 0 periodically.", Boolean.valueOf(false), this.sgBypass);
   private final Setting<Double> factor = this.setting(
      "factor", "Factor.", 1.0, this.sgFactor, () -> this.type.get() == PacketFly.Type.FACTOR || this.type.get() == PacketFly.Type.DESYNC, 1.0, 10.0, 1
   );
   private final Setting<Boolean> factorAntiKick = this.setting(
      "factor-anti-kick", "Whether to factorize the anti kick.", Boolean.valueOf(false), this.sgFactor
   );
   private final Setting<Keybind> factorize = this.setting(
      "factor-overwrite", "Overwrites the factor to allow for bursting.", Keybind.fromKey(-1), this.sgFactor, () -> this.type.get() == PacketFly.Type.FACTOR
   );
   private final Setting<Double> motion = this.setting(
      "overwrite-factor",
      "The factor overwrite when pressing the button.",
      5.0,
      this.sgFactor,
      () -> this.type.get() == PacketFly.Type.FACTOR && ((Keybind)this.factorize.get()).getValue() != -1,
      1.0,
      20.0,
      1
   );
   private final Setting<PacketFly.Phase> phase = this.setting("phase", "Fuck walls all my homies hate walls.", PacketFly.Phase.NCP, this.sgPhase);
   private final Setting<Boolean> multiAxis = this.setting(
      "multi-axis", "Idk.", Boolean.valueOf(false), this.sgPhase, () -> this.phase.get() != PacketFly.Phase.NONE
   );
   private final Setting<Boolean> noPhaseSlow = this.setting(
      "no-phase-slow", "Won't slow you down when phasing.", Boolean.valueOf(false), this.sgPhase, () -> this.phase.get() != PacketFly.Phase.NONE
   );
   private class_2829 startingOutOfBoundsPos;
   private final ArrayList<PlayerMoveC2SPacket> packets = new ArrayList();
   private final Map<Integer, PacketFly.TimeVec3d> posLooks = new ConcurrentHashMap<>();
   private int vDelay;
   private int hDelay;
   private int antiKickTicks;
   private int factorCounter;
   private int limitTicks;
   private int jitterTicks;
   private int teleportId;
   double speedX;
   double speedY;
   double speedZ;
   private boolean limitStrict;
   private boolean oddJitter;
   private final Timer intervalTimer = new Timer();
   private static final Random random = new Random();

   public PacketFly() {
      super(Venomhack420.CATEGORY, "packet-fly", "Allows flight using packets.");
   }

   @EventHandler
   public void onPreTick(Pre event) {
      if (this.mc.player.age % 20 == 0) {
         this.cleanPosLooks();
      }

      this.mc.player.setVelocity(0.0, 0.0, 0.0);
      if (this.teleportId <= 0 && this.type.get() != PacketFly.Type.SETBACK) {
         this.startingOutOfBoundsPos = new class_2829(this.randomHorizontal(), 1.0, this.randomHorizontal(), this.mc.player.isOnGround());
         this.packets.add(this.startingOutOfBoundsPos);
         this.mc.player.networkHandler.sendPacket(this.startingOutOfBoundsPos);
      } else {
         if (!this.timerMoving.get() || this.isMoving()) {
            ((meteordevelopment.meteorclient.systems.modules.world.Timer)Modules.get().get(meteordevelopment.meteorclient.systems.modules.world.Timer.class))
               .setOverride(this.boostTimer.get());
         }

         boolean phasing = this.checkCollisionBox();
         this.speedX = 0.0;
         this.speedY = 0.0;
         this.speedZ = 0.0;
         if (!this.mc.options.jumpKey.isPressed()
            || this.mc.options.sneakKey.isPressed()
            || this.hDelay >= 1 && (!this.multiAxis.get() || !phasing)) {
            if (this.mc.options.sneakKey.isPressed()
               && !this.mc.options.jumpKey.isPressed()
               && (this.hDelay < 1 || this.multiAxis.get() && phasing)) {
               this.speedY = -0.062;
               this.antiKickTicks = 0;
               this.vDelay = 5;
            }
         } else {
            if (this.mc.player.age
                  % (
                     this.type.get() != PacketFly.Type.SETBACK && this.type.get() != PacketFly.Type.SLOW && this.limit.get() != PacketFly.Limit.STRICT
                        ? 20
                        : 10
                  )
               == 0) {
               this.speedY = this.antiKickMode.get() != PacketFly.AntiKick.NONE ? -0.032 : 0.062;
            } else {
               this.speedY = 0.062;
            }

            this.antiKickTicks = 0;
            this.vDelay = 5;
         }

         if (this.multiAxis.get() && phasing || !this.mc.options.sneakKey.isPressed() || !this.mc.options.jumpKey.isPressed()) {
            if (PlayerUtils.isMoving()) {
               double[] dir = this.directionSpeed(
                  (phasing && this.phase.get() == PacketFly.Phase.NCP ? (this.noPhaseSlow.get() ? (this.multiAxis.get() ? 0.0465 : 0.062) : 0.031) : 0.26)
                     * this.speed.get()
               );
               if ((dir[0] != 0.0 || dir[1] != 0.0) && (this.vDelay < 1 || this.multiAxis.get() && phasing)) {
                  this.speedX = dir[0];
                  this.speedZ = dir[1];
                  this.hDelay = 5;
               }
            }

            if (this.antiKickMode.get() != PacketFly.AntiKick.NONE && (this.limit.get() == PacketFly.Limit.NONE || this.limitTicks != 0)) {
               if (this.antiKickTicks < (this.packetMode.get() == PacketFly.Mode.BYPASS && !this.bounds.get() ? 1 : 3)) {
                  ++this.antiKickTicks;
               } else {
                  this.antiKickTicks = 0;
                  if (this.antiKickMode.get() != PacketFly.AntiKick.LIMITED || !phasing) {
                     this.speedY = this.antiKickMode.get() == PacketFly.AntiKick.STRICT ? -0.08 : -0.04;
                  }
               }
            }
         }

         if (phasing
            && (
               this.phase.get() == PacketFly.Phase.NCP && (double)this.mc.player.forwardSpeed != 0.0
                  || (double)this.mc.player.sidewaysSpeed != 0.0 && this.speedY != 0.0
            )) {
            this.speedY /= 2.5;
         }

         if (this.limit.get() != PacketFly.Limit.NONE) {
            if (this.limitTicks == 0) {
               this.speedX = 0.0;
               this.speedY = 0.0;
               this.speedZ = 0.0;
            } else if (this.limitTicks == 2 && this.jitter.get()) {
               if (this.oddJitter) {
                  this.speedX = 0.0;
                  this.speedY = 0.0;
                  this.speedZ = 0.0;
               }

               this.oddJitter = !this.oddJitter;
            }
         } else if (this.jitter.get() && this.jitterTicks == 7) {
            this.speedX = 0.0;
            this.speedY = 0.0;
            this.speedZ = 0.0;
         }

         switch((PacketFly.Type)this.type.get()) {
            case FAST:
               this.mc.player.setVelocity(this.speedX, this.speedY, this.speedZ);
               this.sendPackets(this.speedX, this.speedY, this.speedZ, (PacketFly.Mode)this.packetMode.get(), true);
               break;
            case SLOW:
               this.sendPackets(this.speedX, this.speedY, this.speedZ, (PacketFly.Mode)this.packetMode.get(), true);
               break;
            case SETBACK:
               this.mc.player.setVelocity(this.speedX, this.speedY, this.speedZ);
               this.sendPackets(this.speedX, this.speedY, this.speedZ, (PacketFly.Mode)this.packetMode.get(), false);
               break;
            case FACTOR:
            case DESYNC:
               double rawFactor = this.factor.get();
               if (((Keybind)this.factorize.get()).isPressed() && this.intervalTimer.passedMillis(3500L)) {
                  this.intervalTimer.reset();
                  rawFactor = this.motion.get();
               }

               int factorInt = (int)Math.floor(rawFactor);
               ++this.factorCounter;
               if (this.factorCounter > (int)(20.0 / ((rawFactor - (double)factorInt) * 20.0))) {
                  ++factorInt;
                  this.factorCounter = 0;
               }

               for(int i = 1; i <= factorInt; ++i) {
                  this.mc
                     .player
                     .setVelocity(
                        this.speedX * (double)i,
                        this.speedY
                           * (double)(
                              this.speedY < 0.0
                                    && !this.mc.options.sneakKey.isPressed()
                                    && this.antiKickMode.get() != PacketFly.AntiKick.NONE
                                    && !this.factorAntiKick.get()
                                 ? 1
                                 : i
                           ),
                        this.speedZ * (double)i
                     );
                  this.sendPackets(this.speedX * (double)i, this.speedY * (double)i, this.speedZ * (double)i, (PacketFly.Mode)this.packetMode.get(), true);
               }

               Vec3d v = this.mc.player.getVelocity();
               this.speedX = v.x;
               this.speedY = v.y;
               this.speedZ = v.z;
         }

         --this.vDelay;
         --this.hDelay;
         if (this.constrict.get() && (this.limit.get() == PacketFly.Limit.NONE || this.limitTicks > 1)) {
            this.mc
               .player
               .networkHandler
               .sendPacket(new class_2829(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ(), false));
         }

         ++this.limitTicks;
         ++this.jitterTicks;
         if (this.limitTicks > (this.limit.get() == PacketFly.Limit.STRICT ? (this.limitStrict ? 1 : 2) : 3)) {
            this.limitTicks = 0;
            this.limitStrict = !this.limitStrict;
         }

         if (this.jitterTicks > 7) {
            this.jitterTicks = 0;
         }
      }
   }

   private void sendPackets(double x, double y, double z, PacketFly.Mode mode, boolean sendConfirmTeleport) {
      Vec3d nextPos = new Vec3d(this.mc.player.getX() + x, this.mc.player.getY() + y, this.mc.player.getZ() + z);
      Vec3d bounds = this.getBoundsVec(x, y, z, mode);
      PlayerMoveC2SPacket nextPosPacket = new class_2829(nextPos.x, nextPos.y, nextPos.z, this.mc.player.isOnGround());
      this.packets.add(nextPosPacket);
      this.mc.player.networkHandler.sendPacket(nextPosPacket);
      if (this.limit.get() == PacketFly.Limit.NONE || this.limitTicks != 0) {
         PlayerMoveC2SPacket boundsPacket = new class_2829(bounds.x, bounds.y, bounds.z, this.mc.player.isOnGround());
         this.packets.add(boundsPacket);
         this.mc.player.networkHandler.sendPacket(boundsPacket);
         if (sendConfirmTeleport) {
            ++this.teleportId;
            if (this.extraCt.get()) {
               this.mc.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(this.teleportId - 1));
            }

            this.mc.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(this.teleportId));
            this.posLooks
               .put(this.teleportId, new PacketFly.TimeVec3d(nextPos.x, nextPos.y, nextPos.z, System.currentTimeMillis()));
            if (this.extraCt.get()) {
               this.mc.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(this.teleportId + 1));
            }
         }

         if (this.type.get() != PacketFly.Type.FACTOR && this.packetMode.get() != PacketFly.Mode.BYPASS) {
            PlayerMoveC2SPacket currentPos = new class_2829(
               this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ(), false
            );
            this.packets.add(currentPos);
            this.mc.player.networkHandler.sendPacket(currentPos);
         }
      }
   }

   private Vec3d getBoundsVec(double x, double y, double z, PacketFly.Mode mode) {
      switch(mode) {
         case UP:
            return new Vec3d(
               this.mc.player.getX() + x,
               this.bounds.get() ? (double)(this.strict.get() ? 255 : 256) : this.mc.player.getY() + 420.0,
               this.mc.player.getZ() + z
            );
         case PRESERVE:
            return new Vec3d(
               this.bounds.get() ? this.mc.player.getX() + this.randomHorizontal() : this.randomHorizontal(),
               this.strict.get() ? Math.max(this.mc.player.getY(), 2.0) : this.mc.player.getY(),
               this.bounds.get() ? this.mc.player.getZ() + this.randomHorizontal() : this.randomHorizontal()
            );
         case LIMITJITTER:
            return new Vec3d(
               this.mc.player.getX() + (this.strict.get() ? x : randomLimitedHorizontal()),
               this.mc.player.getY() + randomLimitedVertical(),
               this.mc.player.getZ() + (this.strict.get() ? z : randomLimitedHorizontal())
            );
         case BYPASS:
            if (this.bounds.get()) {
               double rawY = y * 510.0;
               return new Vec3d(
                  this.mc.player.getX() + x,
                  this.mc.player.getY()
                     + (rawY > (double)(PlayerUtils.getDimension() == Dimension.Nether ? 127 : 255) ? -rawY : (rawY < 1.0 ? -rawY : rawY)),
                  this.mc.player.getZ() + z
               );
            }

            return new Vec3d(
               this.mc.player.getX() + (x == 0.0 ? (double)(random.nextBoolean() ? -10 : 10) : x * 38.0),
               this.mc.player.getY() + y,
               this.mc.player.getX() + (z == 0.0 ? (double)(random.nextBoolean() ? -10 : 10) : z * 38.0)
            );
         case OBSCURE:
            return new Vec3d(
               this.mc.player.getX() + this.randomHorizontal(),
               Math.max(1.5, Math.min(this.mc.player.getY() + y, 253.5)),
               this.mc.player.getZ() + this.randomHorizontal()
            );
         default:
            return new Vec3d(
               this.mc.player.getX() + x,
               this.bounds.get() ? (double)(this.strict.get() ? 1 : 0) : this.mc.player.getY() - 1337.0,
               this.mc.player.getZ() + z
            );
      }
   }

   public double randomHorizontal() {
      int randomValue = random.nextInt(
            this.bounds.get() ? 80 : (this.packetMode.get() == PacketFly.Mode.OBSCURE ? (this.mc.player.age % 2 == 0 ? 480 : 100) : 29000000)
         )
         + (this.bounds.get() ? 5 : 500);
      return random.nextBoolean() ? (double)randomValue : (double)(-randomValue);
   }

   public static double randomLimitedVertical() {
      int randomValue = random.nextInt(22);
      randomValue += 70;
      return random.nextBoolean() ? (double)randomValue : (double)(-randomValue);
   }

   public static double randomLimitedHorizontal() {
      int randomValue = random.nextInt(10);
      return random.nextBoolean() ? (double)randomValue : (double)(-randomValue);
   }

   private void cleanPosLooks() {
      this.posLooks.forEach((tp, timeVec3d) -> {
         if (System.currentTimeMillis() - timeVec3d.getTime() > TimeUnit.SECONDS.toMillis(30L)) {
            this.posLooks.remove(tp);
         }
      });
   }

   private boolean isMoving() {
      return this.mc.player.forwardSpeed != 0.0F
         || this.mc.player.sidewaysSpeed != 0.0F
         || this.mc.options.jumpKey.isPressed()
         || this.mc.options.sneakKey.isPressed();
   }

   public void onActivate() {
      this.packets.clear();
      this.posLooks.clear();
      this.teleportId = 0;
      this.vDelay = 0;
      this.hDelay = 0;
      this.antiKickTicks = 0;
      this.limitTicks = 0;
      this.jitterTicks = 0;
      this.speedX = 0.0;
      this.speedY = 0.0;
      this.speedZ = 0.0;
      this.oddJitter = false;
      this.startingOutOfBoundsPos = new class_2829(this.randomHorizontal(), 1.0, this.randomHorizontal(), this.mc.player.isOnGround());
      this.packets.add(this.startingOutOfBoundsPos);
      this.mc.player.networkHandler.sendPacket(this.startingOutOfBoundsPos);
   }

   public void onDeactivate() {
      this.mc.player.setVelocity(0.0, 0.0, 0.0);
      ((meteordevelopment.meteorclient.systems.modules.world.Timer)Modules.get().get(meteordevelopment.meteorclient.systems.modules.world.Timer.class))
         .setOverride(1.0);
   }

   @EventHandler
   public void onReceive(Receive event) {
      Packet vec = event.packet;
      if (vec instanceof PlayerPositionLookS2CPacket packet) {
         if (!(this.mc.currentScreen instanceof DownloadingTerrainScreen)) {
            if (this.mc.player.isAlive()) {
               if (this.teleportId <= 0) {
                  this.teleportId = ((PlayerPositionLookS2CPacket)event.packet).getTeleportId();
               } else if (this.mc.world.isChunkLoaded(this.mc.player.getChunkPos().x, this.mc.player.getChunkPos().z)
                  && this.type.get() != PacketFly.Type.SETBACK) {
                  if (this.type.get() == PacketFly.Type.DESYNC) {
                     this.posLooks.remove(packet.getTeleportId());
                     event.cancel();
                     if (this.type.get() == PacketFly.Type.SLOW) {
                        this.mc.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
                     }

                     return;
                  }

                  if (this.posLooks.containsKey(packet.getTeleportId())) {
                     PacketFly.TimeVec3d vecx = (PacketFly.TimeVec3d)this.posLooks.get(packet.getTeleportId());
                     if (vecx.x == packet.getX() && vecx.y == packet.getY() && vecx.z == packet.getZ()) {
                        this.posLooks.remove(packet.getTeleportId());
                        event.cancel();
                        if (this.type.get() == PacketFly.Type.SLOW) {
                           this.mc.player.setPosition(packet.getX(), packet.getY(), packet.getZ());
                        }

                        return;
                     }
                  }
               }
            }

            ((PlayerPositionLookS2CPacketAccessor)packet).setYaw(this.mc.player.getYaw());
            ((PlayerPositionLookS2CPacketAccessor)packet).setPitch(this.mc.player.getPitch());
            packet.getFlags().remove(class_2709.X_ROT);
            packet.getFlags().remove(class_2709.Y_ROT);
            this.teleportId = packet.getTeleportId();
         } else {
            this.teleportId = 0;
         }
      }
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      if (this.type.get() == PacketFly.Type.SETBACK || this.teleportId > 0) {
         if (this.type.get() != PacketFly.Type.SLOW) {
            ((IVec3d)event.movement).setXZ(this.speedX, this.speedZ);
            ((IVec3d)event.movement).setY(this.speedY);
         }

         if (this.phase.get() != PacketFly.Phase.NONE && this.phase.get() == PacketFly.Phase.VANILLA || this.checkCollisionBox()) {
            this.mc.player.noClip = true;
         }
      }
   }

   private boolean checkCollisionBox() {
      return this.mc.world.getBlockCollisions(this.mc.player, this.mc.player.getBoundingBox()).iterator().hasNext()
         ? true
         : this.mc
            .world
            .getBlockCollisions(this.mc.player, this.mc.player.getBoundingBox().offset(0.0, 2.0, 0.0).contract(0.0, 1.99, 0.0))
            .iterator()
            .hasNext();
   }

   @EventHandler
   public void onSend(Send event) {
      Packet var3 = event.packet;
      if (var3 instanceof PlayerMoveC2SPacket packet) {
         if (!(event.packet instanceof class_2829)) {
            event.cancel();
         }

         if (this.packets.contains(packet)) {
            this.packets.remove(packet);
            return;
         }

         event.cancel();
      }
   }

   public String getInfoString() {
      return this.type.name;
   }

   public double[] directionSpeed(double speed) {
      float forward = this.mc.player.forwardSpeed;
      float side = this.mc.player.sidewaysSpeed;
      float yaw = this.mc.player.prevYaw + (this.mc.player.getYaw() - this.mc.player.prevYaw) * this.mc.getTickDelta();
      if (forward != 0.0F) {
         if (side > 0.0F) {
            yaw += (float)(forward > 0.0F ? -45 : 45);
         } else if (side < 0.0F) {
            yaw += (float)(forward > 0.0F ? 45 : -45);
         }

         side = 0.0F;
         if (forward > 0.0F) {
            forward = 1.0F;
         } else if (forward < 0.0F) {
            forward = -1.0F;
         }
      }

      double sin = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
      double cos = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
      double posX = (double)forward * speed * cos + (double)side * speed * sin;
      double posZ = (double)forward * speed * sin - (double)side * speed * cos;
      return new double[]{posX, posZ};
   }

   public static enum AntiKick {
      NONE,
      NORMAL,
      LIMITED,
      STRICT;
   }

   public static enum Limit {
      NONE,
      STRONG,
      STRICT;
   }

   public static enum Mode {
      UP,
      PRESERVE,
      DOWN,
      LIMITJITTER,
      BYPASS,
      OBSCURE;
   }

   public static enum Phase {
      NONE,
      VANILLA,
      NCP;
   }

   private static class TimeVec3d extends Vec3d {
      private final long time;

      public TimeVec3d(double xIn, double yIn, double zIn, long time) {
         super(xIn, yIn, zIn);
         this.time = time;
      }

      public long getTime() {
         return this.time;
      }
   }

   public static enum Type {
      FACTOR,
      SETBACK,
      FAST,
      SLOW,
      DESYNC;
   }
}
