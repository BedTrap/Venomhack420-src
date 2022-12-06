package venomhack.modules.combat;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent.Post;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Sent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.world.World;
import net.minecraft.block.BedBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.network.Packet;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos.class_2339;
import net.minecraft.util.math.Direction.class_2351;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class_2829;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class_2831;
import org.jetbrains.annotations.Nullable;
import venomhack.Venomhack420;
import venomhack.enums.SurroundBlocks;
import venomhack.modules.ModuleHelper;
import venomhack.utils.BlockUtils2;
import venomhack.utils.PlayerUtils2;
import venomhack.utils.RandUtils;
import venomhack.utils.UtilsPlus;

public class Burrow extends ModuleHelper {
   private final SettingGroup sgExtra = this.group("Extra");
   private final SettingGroup sgAutomation = this.group("Automation");
   private final Setting<SurroundBlocks> block = this.setting("block-to-use", "The block to use for Burrow.", SurroundBlocks.OBSIDIAN);
   private final Setting<Integer> minRubberbandHeight = this.setting(
      "min-rubberband-height", "Maximum blocks to teleport up or down to cause a rubberband.", Integer.valueOf(4), -20.0, 20.0
   );
   private final Setting<Integer> maxRubberbandHeight = this.setting(
      "max-rubberband-height", "Minimum blocks to teleport up or down to cause a rubberband.", Integer.valueOf(8), -20.0, 20.0
   );
   private final Setting<Integer> minBlacklist = this.setting("min-blacklist-height", "Start of the blacklisted area.", Integer.valueOf(2), -10.0, 10.0);
   private final Setting<Integer> maxBlacklist = this.setting("max-blacklist-height", "End of the blacklisted area.", Integer.valueOf(-2), -10.0, 10.0);
   private final Setting<Boolean> attackCrystals = this.setting(
      "attack-crystals", "Whether to attack crystals that are in the way.", Boolean.valueOf(true), this.sgExtra
   );
   public final Setting<Boolean> phoenixMode = this.setting(
      "phoenix-mode", "Allows you to burrow with a block above your head. Works only on pa.", Boolean.valueOf(false), this.sgExtra
   );
   private final Setting<Boolean> center = this.setting(
      "center", "Centers you to the middle of the block before burrowing.", Boolean.valueOf(true), this.sgExtra
   );
   private final Setting<Boolean> hardSnap = this.setting(
      "Hard-Center", "Will align you at the exact center of your hole.", Boolean.valueOf(false), this.sgExtra, this.center::get
   );
   private final Setting<Boolean> strictDirections = this.setting("strict-directions", "Places only on visible sides.", Boolean.valueOf(false), this.sgExtra);
   private final Setting<Boolean> rotate = this.setting("rotate", "Faces the block you place server-side.", Boolean.valueOf(false), this.sgExtra);
   private final Setting<Integer> pitchStep = this.setting(
      "max-yawstep", "How far to rotate with each step.", Integer.valueOf(180), this.sgExtra, this.rotate::get, 1.0, 180.0, 1, 180
   );
   private final Setting<Boolean> swing = this.setting("swing", "Whether to swing your hand client side or not.", Boolean.valueOf(false), this.sgExtra);
   private final Setting<Boolean> airPlace = this.setting("air-place", "Whether to place in midair or not.", Boolean.valueOf(true), this.sgExtra);
   public final Setting<Boolean> autoTrap = this.setting(
      "auto-burrow-trap",
      "Automatically activates burrow if someone is about to jump into your hole.",
      Boolean.valueOf(false),
      this.sgAutomation,
      this::handleListener
   );
   public final Setting<Boolean> autoReburrow = this.setting(
      "auto-burrow-replenish",
      "Automatically burrows you again when someone mines your burrow block.",
      Boolean.valueOf(false),
      this.sgAutomation,
      this::handleListener
   );
   public final Setting<Boolean> pauseEating = this.setting(
      "pause-while-eating",
      "Will not automatically burrow you again when you are eating.",
      Boolean.valueOf(true),
      this.sgAutomation,
      () -> this.autoReburrow.get() || this.autoTrap.get()
   );
   private final List<Double> packetList = new ArrayList<>(6);
   private final class_2339 mutable = new class_2339();
   private Vec3d playerPos;
   private FindItemResult result;
   private BlockHitResult hitResult;
   private BlockPos playerBlock;
   private BlockState state;
   @Nullable
   private Entity entity;
   private float serverPitch;
   private static final int CHAT_ID = 972483264;
   private final Burrow.StaticListener BURROW_LISTENER = new Burrow.StaticListener();

   public Burrow() {
      super(Venomhack420.CATEGORY, "burrow-vh", "Attempts to place a block inside of your feet.");
      this.handleListener(true);
   }

   public void onActivate() {
      if (this.mc.player != null) {
         this.serverPitch = this.mc.player.getPitch();
      }

      if (!this.mc.player.isOnGround()) {
         this.toggleWithError(972483264, Text.translatable("burrow.ground"));
      } else {
         FindItemResult result = ((SurroundBlocks)this.block.get()).getBlock();
         if ((result.isHotbar() || result.isOffhand()) && result.found()) {
            this.playerPos = this.mc.player.getPos();
            if (this.center.get()) {
               this.playerPos = BlockUtils2.getCenterPos(this.hardSnap.get());
            }

            boolean onEchest = this.playerPos.y != Math.ceil(this.playerPos.y);
            BlockPos playerBlock = new BlockPos(
               this.playerPos.x, onEchest ? Math.ceil(this.playerPos.y) : this.playerPos.y, this.playerPos.z
            );
            if (BlockUtils2.invalidPos(playerBlock)) {
               this.toggleWithError(972483264, Text.translatable("burrow.world"));
            } else {
               BlockHitResult hitResult = BlockUtils2.getPlaceResult(playerBlock, this.airPlace.get(), this.strictDirections.get());
               if (hitResult == null) {
                  this.toggleWithError(972483264, Text.translatable("burrow.place"));
               } else {
                  float eyeHeight = this.mc.player.getEyeHeight(this.mc.player.getPose());
                  if (!this.phoenixMode.get() && this.collides(Math.ceil((double)eyeHeight))) {
                     this.toggleWithError(972483264, Text.translatable("burrow.headroom"));
                  } else {
                     BlockState state = this.mc.world.getBlockState(playerBlock);
                     if (!state.getMaterial().isReplaceable()
                        && (!(state.getBlock() instanceof BedBlock) || this.mc.world.getRegistryKey() == World.OVERWORLD)
                        && state.getBlock().getHardness() > 0.0F
                        && this.collides(onEchest ? 1.0 : 0.0)) {
                        this.toggleWithError(972483264, Text.translatable("burrow.burrowed"));
                     } else {
                        VoxelShape placeShape = ((BlockItem)PlayerUtils2.getItemFromResult(result))
                           .getBlock()
                           .getDefaultState()
                           .getCollisionShape(this.mc.world, playerBlock);
                        Entity entity = this.getEntityInDaWay(placeShape, playerBlock);
                        if (entity == null || this.attackCrystals.get() && entity instanceof EndCrystalEntity) {
                           this.packetList.clear();

                           for(int rubberHeight = this.minRubberbandHeight.get(); rubberHeight <= this.maxRubberbandHeight.get(); ++rubberHeight) {
                              if ((rubberHeight < this.minBlacklist.get() || rubberHeight > this.maxBlacklist.get())
                                 && (rubberHeight <= -3 || rubberHeight >= 2)
                                 && !this.collides((double)rubberHeight)) {
                                 if (!this.collides((double)rubberHeight + Math.floor((double)eyeHeight))) {
                                    if (this.phoenixMode.get() && this.collides(Math.ceil((double)eyeHeight))) {
                                       boolean cantJump = true;

                                       for(double i = -4.0; i < 5.0; ++i) {
                                          if (i != 0.0 && i != -1.0 && !this.collides(i)) {
                                             if (this.collides(i + Math.floor((double)eyeHeight))) {
                                                if (eyeHeight > 1.0F) {
                                                   ++i;
                                                }
                                             } else {
                                                boolean cantRubberband = false;
                                                if ((double)rubberHeight - i < 5.0 && i - (double)rubberHeight < 9.0) {
                                                   cantRubberband = true;

                                                   for(int j = rubberHeight; j <= this.maxRubberbandHeight.get(); ++j) {
                                                      if ((j < this.minBlacklist.get() || j > this.maxBlacklist.get())
                                                         && (j <= -3 || j >= 3)
                                                         && (!((double)j - i < 5.0) || !(i - (double)j < 9.0))
                                                         && !this.collides((double)j)) {
                                                         if (!this.collides((double)j + Math.floor((double)eyeHeight))) {
                                                            rubberHeight = j;
                                                            cantRubberband = false;
                                                            break;
                                                         }

                                                         if (eyeHeight > 1.0F) {
                                                            ++j;
                                                         }
                                                      }
                                                   }
                                                }

                                                if (!cantRubberband) {
                                                   cantJump = false;
                                                   if (this.collides(i - 1.0)) {
                                                      if (i == 4.0) {
                                                         this.packetList.add(0.025);
                                                      }

                                                      i += 0.025;
                                                   }

                                                   this.packetList.add(i);
                                                   break;
                                                }
                                             }
                                          }
                                       }

                                       if (cantJump) {
                                          this.toggleWithError(972483264, Text.translatable("burrow.headroom"));
                                          return;
                                       }
                                    } else {
                                       this.packetList.add(0.42);
                                       this.packetList.add(0.75);
                                       this.packetList.add(1.01);
                                       this.packetList.add(1.15);
                                       if (onEchest) {
                                          double maxY = placeShape.offset(
                                                (double)playerBlock.getX(), (double)playerBlock.getY(), (double)playerBlock.getZ()
                                             )
                                             .getMax(class_2351.Y);
                                          if (this.playerPos.y + 1.15 <= maxY) {
                                             this.packetList.add(maxY - this.playerPos.y + 0.025);
                                          }
                                       }
                                    }

                                    this.packetList.add((double)rubberHeight);
                                    if (this.rotate.get()) {
                                       this.result = result;
                                       this.hitResult = hitResult;
                                       this.playerBlock = playerBlock;
                                       this.state = state;
                                       this.entity = entity;
                                    } else {
                                       this.performBurrow(result, hitResult, playerBlock, state, entity);
                                    }

                                    return;
                                 }

                                 if (eyeHeight > 1.0F) {
                                    ++rubberHeight;
                                 }
                              }
                           }

                           this.toggleWithError(972483264, Text.translatable("burrow.rubberband"));
                        } else {
                           this.toggleWithError(972483264, Text.translatable("burrow.entities"));
                        }
                     }
                  }
               }
            }
         } else {
            this.toggleWithError(972483264, Text.translatable("burrow.block"));
         }
      }
   }

   @EventHandler
   private void onPacketSent(Sent event) {
      Packet var3 = event.packet;
      if (var3 instanceof PlayerMoveC2SPacket packet) {
         this.serverPitch = packet.getPitch(this.serverPitch);
      }
   }

   @EventHandler(
      priority = -200
   )
   private void onLateTick(Pre event) {
      if (this.rotate.get() && this.playerBlock != null) {
         this.mc.player.networkHandler.sendPacket(new class_2831(this.mc.player.getYaw(), 90.0F, this.mc.player.isOnGround()));
         this.performBurrow(this.result, this.hitResult, this.playerBlock, this.state, this.entity);
         double targetPitch = Rotations.getPitch(this.hitResult.getPos());
         double pitchDist = Math.abs((double)this.serverPitch - targetPitch);
         if (pitchDist <= (double)((Integer)this.pitchStep.get()).intValue()) {
            Rotations.rotate(
               (double)this.mc.player.getYaw(),
               targetPitch,
               99,
               () -> this.performBurrow(this.result, this.hitResult, this.playerBlock, this.state, this.entity)
            );
         } else {
            float pitch = this.serverPitch;
            if (pitchDist > (double)((Integer)this.pitchStep.get()).intValue()) {
               pitchDist = (double)((Integer)this.pitchStep.get()).intValue();
            }

            if (targetPitch > (double)this.serverPitch) {
               pitch = (float)((double)pitch + pitchDist);
            } else {
               pitch = (float)((double)pitch - pitchDist);
            }

            pitch = Math.min(Math.max(-90.0F, pitch), 90.0F);
            Rotations.rotate((double)this.mc.player.getYaw(), (double)pitch);
         }
      }
   }

   private void performBurrow(FindItemResult result, BlockHitResult hitResult, BlockPos playerBlock, BlockState state, @Nullable Entity entity) {
      if (this.center.get()) {
         this.mc.player.setPosition(this.playerPos);
      }

      for(int i = 0; i < this.packetList.size(); ++i) {
         double height = this.packetList.get(i);
         if (i < this.packetList.size() - 1) {
            this.mc
               .player
               .networkHandler
               .sendPacket(
                  new class_2829(this.mc.player.getX(), this.mc.player.getY() + height, this.mc.player.getZ(), true)
               );
         } else {
            if (entity != null) {
               this.mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, this.mc.player.isSneaking()));
               RandUtils.swing(this.swing.get(), RandUtils.hand(result));
            }

            if (state.getBlock() instanceof BedBlock && this.mc.world.getRegistryKey() != World.OVERWORLD) {
               this.mc
                  .player
                  .networkHandler
                  .sendPacket(
                     new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(playerBlock), Direction.UP, playerBlock, true), 0)
                  );
            } else if (state.getBlock().getHardness() == 0.0F && !state.isAir()) {
               UtilsPlus.mine(playerBlock, false, false);
            }

            BlockUtils2.justPlace(result, hitResult, this.swing.get(), false, 0);
            this.mc
               .player
               .networkHandler
               .sendPacket(
                  new class_2829(this.mc.player.getX(), this.mc.player.getY() + height, this.mc.player.getZ(), false)
               );
            this.toggle();
         }
      }
   }

   @Nullable
   private Entity getEntityInDaWay(VoxelShape placeShape, BlockPos playerBlock) {
      EndCrystalEntity crystal = null;

      try {
         for(Entity entity : this.mc
            .world
            .getOtherEntities(
               this.mc.player,
               placeShape.isEmpty() ? new Box(playerBlock) : placeShape.getBoundingBox().offset(playerBlock),
               Entity::isCollidable
            )) {
            if (!(entity instanceof EndCrystalEntity)) {
               return entity;
            }

            EndCrystalEntity c = (EndCrystalEntity)entity;
            crystal = c;
         }
      } catch (ConcurrentModificationException var7) {
      }

      return crystal;
   }

   private boolean collides(double yOffset) {
      return this.mc
            .world
            .getBlockState(this.mutable.set(this.playerPos.x + 0.3, this.playerPos.y + yOffset, this.playerPos.z + 0.3))
            .getBlock()
            .collidable
         || this.mc
            .world
            .getBlockState(this.mutable.set(this.playerPos.x + 0.3, this.playerPos.y + yOffset, this.playerPos.z - 0.3))
            .getBlock()
            .collidable
         || this.mc
            .world
            .getBlockState(this.mutable.set(this.playerPos.x - 0.3, this.playerPos.y + yOffset, this.playerPos.z + 0.3))
            .getBlock()
            .collidable
         || this.mc
            .world
            .getBlockState(this.mutable.set(this.playerPos.x - 0.3, this.playerPos.y + yOffset, this.playerPos.z - 0.3))
            .getBlock()
            .collidable;
   }

   public void onDeactivate() {
      this.playerBlock = null;
      if (this.mc.player != null) {
         this.serverPitch = this.mc.player.getPitch();
      }
   }

   private void handleListener(boolean ignored) {
      if (!this.autoTrap.get() && !this.autoReburrow.get()) {
         MeteorClient.EVENT_BUS.unsubscribe(this.BURROW_LISTENER);
      } else {
         MeteorClient.EVENT_BUS.subscribe(this.BURROW_LISTENER);
      }
   }

   private class StaticListener {
      private BlockPos pos = null;
      private int delay;

      @EventHandler
      private void surroundListener(Post event) {
         --this.delay;
         if (UtilsPlus.isObbyBurrowed(Burrow.this.mc.player)
            && Burrow.this.mc.world.getBlockState(Burrow.this.mc.player.getBlockPos()).getBlock().getBlastResistance() > 600.0F) {
            this.pos = Burrow.this.mc.player.getBlockPos();
         } else if (!Burrow.this.isActive()
            && Burrow.this.mc.player.isOnGround()
            && !UtilsPlus.isBurrowed(Burrow.this.mc.player)
            && (
               !Burrow.this.pauseEating.get()
                  || !Burrow.this.mc.player.isUsingItem()
                  || !Burrow.this.mc.player.getMainHandStack().isFood() && !Burrow.this.mc.player.getOffHandStack().isFood()
            )) {
            if (Burrow.this.autoReburrow.get() && Burrow.this.mc.player.getBlockPos().equals(this.pos)) {
               this.pos = null;
               Burrow.this.toggle();
            } else {
               if (this.delay <= 0 && Burrow.this.autoTrap.get() && UtilsPlus.isSurrounded(Burrow.this.mc.player, false, false)) {
                  for(PlayerEntity enemy : Burrow.this.mc.world.getPlayers()) {
                     if (!Burrow.this.mc.player.equals(enemy) && !(Burrow.this.mc.player.distanceTo(enemy) > 5.0F)) {
                        if (Burrow.this.mc.player.getBlockPos().equals(enemy.getBlockPos())) {
                           break;
                        }

                        if (Friends.get().shouldAttack(enemy)
                           && !UtilsPlus.isSurrounded(enemy, true, true)
                           && Burrow.this.mc.player.getPos().add(0.0, 1.0, 0.0).distanceTo(enemy.getPos()) <= 2.0
                           && enemy.getY() > Burrow.this.mc.player.getY()) {
                           Burrow.this.toggle();
                           this.delay = 20;
                           return;
                        }
                     }
                  }
               }

               this.pos = null;
            }
         }
      }
   }
}
