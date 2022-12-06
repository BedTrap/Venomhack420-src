package venomhack.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Step;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class_2829;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;
import venomhack.utils.UtilsPlus;

public class Anchor extends ModuleHelper {
   private final Setting<Integer> maxHeight = this.setting("max-height", "The maximum height Anchor will work at.", Integer.valueOf(10), 0.0, 20.0);
   private final Setting<Integer> minPitch = this.setting("min-pitch", "The minimum pitch at which anchor will work.", Integer.valueOf(0), -90.0, 90.0);
   private final Setting<Boolean> drag = this.setting("drag", "Will try to drag you into the hole when you stand at the edge of it.", Boolean.valueOf(true));
   private final Setting<Boolean> cancelMove = this.setting("cancel-jump-in-hole", "Prevents you from jumping when Anchor is active.", Boolean.valueOf(false));
   private final Setting<Boolean> cancelStep = this.setting(
      "cancel-step-in-hole", "Prevents you from stepping out of a hole when Anchor is active.", Boolean.valueOf(true)
   );
   private final Setting<Boolean> toggleStep = this.setting("toggle-step", "Toggles step when you try to get into a hole.", Boolean.valueOf(false));
   private final Setting<Boolean> pull = this.setting("pull", "Will pull you at a set speed towards the hole.", Boolean.valueOf(false));
   private final Setting<Double> pullSpeed = this.setting("pull-speed", "Your downwards momentum in meters / second.", Double.valueOf(2.0), this.pull::get);
   private final Setting<Boolean> instantCenter = this.setting(
      "instant-center", "Instantly moves you to the center of the hole, useful when stuck on the edge of a hole", Boolean.valueOf(false)
   );
   private final Setting<Boolean> fastEnter = this.setting("fast-enter", "Makes you fall into your hole quicker", Boolean.valueOf(false));
   private final Setting<Double> fastEnterSpeed = this.setting(
      "fast-enter-speed", "the speed at which you will be pulled into the hole", Double.valueOf(5.0), this.fastEnter::get
   );
   private final Setting<Boolean> doubleHoles = this.setting("double-holes", "Anchors you to double holes as well.", Boolean.valueOf(true));
   private final Setting<Boolean> maintainInDoubles = this.setting(
      "maintain-in-doubles", "Keeps you centered in double holes even once you're in them.", Boolean.valueOf(false)
   );
   private boolean wasInHole;
   private boolean foundHole;
   private int holeX1;
   private int holeZ1;
   private int holeX2;
   private int holeZ2;
   public boolean cancelJump;
   public boolean controlMovement;
   public double deltaX;
   public double deltaZ;

   public Anchor() {
      super(Venomhack420.CATEGORY, "anchor-vh", "Better anchor that crashes if i overwrite meteor's one, therefore cope.");
   }

   public void onActivate() {
      this.wasInHole = false;
      this.holeX1 = this.holeZ1 = this.holeX2 = this.holeZ2 = 0;
   }

   @EventHandler
   private void onPreTick(Pre event) {
      this.cancelJump = this.foundHole && this.cancelMove.get() && this.mc.player.getPitch() >= (float)((Integer)this.minPitch.get()).intValue();
   }

   @EventHandler
   private void onPostTick(Post event) {
      this.controlMovement = false;
      int x = MathHelper.floor(this.mc.player.getX());
      int y = MathHelper.floor(this.mc.player.getY());
      int z = MathHelper.floor(this.mc.player.getZ());
      this.foundHole = false;
      double holeX = 0.0;
      double holeZ = 0.0;
      int misc1 = 0;
      Direction air1 = null;
      BlockPos pos1 = new BlockPos(x, y, z);

      for(Direction direction : Direction.values()) {
         if (direction != Direction.UP) {
            BlockState state = this.mc.world.getBlockState(pos1.offset(direction));
            if (state.getBlock().getBlastResistance() >= 600.0F) {
               ++misc1;
            } else if (direction != Direction.DOWN && this.validHole(pos1.offset(direction)) && air1 == null && this.doubleHoles.get()) {
               for(Direction dir : Direction.values()) {
                  if (dir != direction.getOpposite() && dir != Direction.UP) {
                     BlockState blockState1 = this.mc.world.getBlockState(pos1.offset(direction).offset(dir));
                     if (blockState1.getBlock().getBlastResistance() >= 600.0F) {
                        ++misc1;
                     }
                  }
               }

               air1 = direction;
            }
         }
      }

      if (misc1 == 5 && air1 == null) {
         this.wasInHole = true;
         this.holeX1 = x;
         this.holeZ1 = z;
         this.holeX2 = x;
         this.holeZ2 = z;
      } else {
         if (misc1 == 8 && this.doubleHoles.get() && air1 != null) {
            this.wasInHole = true;
            this.holeX1 = x;
            this.holeZ1 = z;
            this.holeX2 = x + air1.getOffsetX();
            this.holeZ2 = z + air1.getOffsetZ();
            if (!this.maintainInDoubles.get()) {
               return;
            }

            holeX = (double)x + 0.5 + (double)air1.getOffsetX();
            holeZ = (double)z + 0.5 + (double)air1.getOffsetZ();
            this.foundHole = true;
         }

         if (!this.wasInHole
            || this.holeX1 != x && this.holeX2 != x
            || this.holeZ1 != z && this.holeZ2 != z
            || (this.holeX1 != this.holeX2 || this.holeZ1 != this.holeZ2) && this.maintainInDoubles.get()) {
            if (this.wasInHole) {
               this.wasInHole = false;
            }

            if (!(this.mc.player.getPitch() < (float)((Integer)this.minPitch.get()).intValue())) {
               if (!this.foundHole) {
                  for(int i = 0; i < this.maxHeight.get(); ++i) {
                     --y;
                     if (y <= this.mc.world.getBottomY() || !this.validHole(new BlockPos(x, y, z))) {
                        break;
                     }

                     int misc = 0;
                     Direction air = null;
                     BlockPos pos = new BlockPos(x, y, z);

                     for(Direction direction : Direction.values()) {
                        if (direction != Direction.UP) {
                           BlockState state = this.mc.world.getBlockState(pos.offset(direction));
                           if (state.getBlock().getBlastResistance() >= 600.0F) {
                              ++misc;
                           } else if (direction != Direction.DOWN
                              && this.validHole(pos.offset(direction))
                              && air == null
                              && this.doubleHoles.get()) {
                              for(Direction dir : Direction.values()) {
                                 if (dir != direction.getOpposite() && dir != Direction.UP) {
                                    BlockState blockState1 = this.mc.world.getBlockState(pos.offset(direction).offset(dir));
                                    if (blockState1.getBlock().getBlastResistance() >= 600.0F) {
                                       ++misc;
                                    }
                                 }
                              }

                              air = direction;
                           }
                        }
                     }

                     if (misc == 5 && air == null) {
                        this.foundHole = true;
                        holeX = (double)x + 0.5;
                        holeZ = (double)z + 0.5;
                        break;
                     }

                     if (misc == 8 && this.doubleHoles.get() && air != null && this.hasAir(pos) && this.hasAir(pos.offset(air))) {
                        this.foundHole = true;
                        holeX = (double)x + 0.5 + (double)air.getOffsetX();
                        holeZ = (double)z + 0.5 + (double)air.getOffsetZ();
                        break;
                     }
                  }
               }

               if (this.foundHole) {
                  Step step = (Step)Modules.get().get(Step.class);
                  if (this.toggleStep.get() && step.isActive()) {
                     step.toggle();
                  }

                  this.controlMovement = true;
                  if (this.drag.get()) {
                     this.deltaX = Utils.clamp(holeX - this.mc.player.getX(), -0.05, 0.05);
                     this.deltaZ = Utils.clamp(holeZ - this.mc.player.getZ(), -0.05, 0.05);
                  } else {
                     if (this.mc.player.getX() < holeX) {
                        if (this.mc.player.getX() < (double)x + 0.31) {
                           return;
                        }
                     } else if (this.mc.player.getX() >= (double)x + 0.69) {
                        return;
                     }

                     if (this.mc.player.getZ() < holeZ) {
                        if (this.mc.player.getZ() < (double)z + 0.31) {
                           return;
                        }
                     } else if (this.mc.player.getZ() >= (double)z + 0.69) {
                        return;
                     }

                     this.deltaX = 0.0;
                     this.deltaZ = 0.0;
                  }

                  ((IVec3d)this.mc.player.getVelocity())
                     .set(this.deltaX, this.pull.get() ? -this.pullSpeed.get() / 20.0 : this.mc.player.getVelocity().y, this.deltaZ);
                  if (this.instantCenter.get()) {
                     this.mc.player.updatePosition(holeX, this.mc.player.getY(), holeZ);
                     this.mc
                        .player
                        .networkHandler
                        .sendPacket(
                           new class_2829(
                              this.mc.player.getX(),
                              this.mc.player.getY(),
                              this.mc.player.getZ(),
                              this.mc.player.isOnGround()
                           )
                        );
                  }

                  if (this.fastEnter.get()) {
                     this.mc
                        .player
                        .setVelocity(this.mc.player.getVelocity().x, -this.fastEnterSpeed.get(), this.mc.player.getVelocity().z);
                  }
               }
            }
         }
      }
   }

   public boolean cancelStep() {
      return this.isActive()
         && this.cancelStep.get()
         && this.mc.player.getPitch() >= (float)((Integer)this.minPitch.get()).intValue()
         && UtilsPlus.isSurrounded(this.mc.player, true, true);
   }

   private boolean hasAir(BlockPos pos) {
      boolean hasair = true;

      for(int i = 0; i < this.maxHeight.get() - 1; ++i) {
         if (!this.validHole(pos.add(0, i, 0))) {
            hasair = false;
            break;
         }
      }

      return hasair;
   }

   private boolean validHole(BlockPos pos) {
      return !this.mc.world.getBlockState(pos).getBlock().collidable;
   }
}
