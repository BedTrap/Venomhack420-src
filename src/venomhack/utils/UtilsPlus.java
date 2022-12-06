package venomhack.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Box2;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos.class_2339;
import net.minecraft.util.math.Direction.class_2351;
import net.minecraft.util.hit.HitResult.class_240;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.class_2847;
import net.minecraft.world.RaycastContext.class_242;
import net.minecraft.world.RaycastContext.class_3960;
import org.jetbrains.annotations.Nullable;
import venomhack.enums.Origin;
import venomhack.modules.misc.PacketMine;

public class UtilsPlus extends Utils {
   private static final class_2339 MUTABLE = new class_2339();
   public static Box2[] CITY_WITH_BURROW = new Box2[]{
      new Box2(0, 0, 0), new Box2(1, 0, 0), new Box2(-1, 0, 0), new Box2(0, 0, 1), new Box2(0, 0, -1)
   };

   public static boolean isSurrounded(LivingEntity target, boolean doubles, boolean onlyBlastProof) {
      BlockPos blockPos = target.getBlockPos();
      int air = 0;

      for(Direction direction : Direction.values()) {
         if (direction != Direction.UP) {
            BlockState state = MeteorClient.mc.world.getBlockState(blockPos.offset(direction));
            if (state.getMaterial().isReplaceable() || onlyBlastProof && state.getBlock().getBlastResistance() < 600.0F) {
               if (!doubles || direction == Direction.DOWN) {
                  return false;
               }

               ++air;

               for(Direction dir : Direction.values()) {
                  if (dir != direction.getOpposite() && dir != Direction.UP) {
                     BlockState state2 = MeteorClient.mc.world.getBlockState(blockPos.offset(direction).offset(dir));
                     if (state2.getMaterial().isReplaceable() || onlyBlastProof && state2.getBlock().getBlastResistance() < 600.0F) {
                        return false;
                     }
                  }
               }
            }
         }
      }

      return air < 2;
   }

   public static boolean isTrapped(LivingEntity target) {
      for(Box2 city : CityUtils.CITY_WITHOUT_BURROW) {
         BlockState state = MeteorClient.mc.world.getBlockState(target.getBlockPos().add(city).up());
         if (state.getMaterial().isReplaceable() || state.getBlock() instanceof BedBlock) {
            return false;
         }
      }

      BlockState state = MeteorClient.mc.world.getBlockState(target.getBlockPos().up(2));
      return !state.getMaterial().isReplaceable() && !(state.getBlock() instanceof BedBlock) && isSurrounded(target, false, false);
   }

   public static boolean isSafe(LivingEntity player) {
      return isSurrounded(player, true, true) || isBurrowed(player);
   }

   public static void mine(BlockPos blockPos, boolean swing, boolean rotate) {
      if (rotate) {
         Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mine(blockPos, swing, false));
      } else {
         MeteorClient.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.START_DESTROY_BLOCK, blockPos, Direction.UP));
         MeteorClient.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
         if (swing) {
            MeteorClient.mc.player.swingHand(Hand.MAIN_HAND);
         } else {
            MeteorClient.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }
      }
   }

   public static int sort(Entity e1, Entity e2, SortPriority priority) {
      return switch(priority) {
         case LowestDistance -> Double.compare((double)e1.distanceTo(MeteorClient.mc.player), (double)e2.distanceTo(MeteorClient.mc.player));
         case HighestDistance -> invertSort(
         Double.compare((double)e1.distanceTo(MeteorClient.mc.player), (double)e2.distanceTo(MeteorClient.mc.player))
      );
         case LowestHealth -> sortHealth(e1, e2);
         case HighestHealth -> invertSort(sortHealth(e1, e2));
         case ClosestAngle -> sortAngle(e1, e2);
         default -> throw new IncompatibleClassChangeError();
      };
   }

   private static int sortHealth(Entity e1, Entity e2) {
      boolean e1l = e1 instanceof LivingEntity;
      boolean e2l = e2 instanceof LivingEntity;
      if (!e1l && !e2l) {
         return 0;
      } else if (e1l && !e2l) {
         return 1;
      } else {
         return !e1l ? -1 : Float.compare(((LivingEntity)e1).getHealth(), ((LivingEntity)e2).getHealth());
      }
   }

   private static int sortAngle(Entity e1, Entity e2) {
      boolean e1l = e1 instanceof LivingEntity;
      boolean e2l = e2 instanceof LivingEntity;
      if (!e1l && !e2l) {
         return 0;
      } else if (e1l && !e2l) {
         return 1;
      } else if (!e1l) {
         return -1;
      } else {
         double e1yaw = Math.abs(Rotations.getYaw(e1) - (double)MeteorClient.mc.player.getYaw());
         double e2yaw = Math.abs(Rotations.getYaw(e2) - (double)MeteorClient.mc.player.getYaw());
         double e1pitch = Math.abs(Rotations.getPitch(e1) - (double)MeteorClient.mc.player.getPitch());
         double e2pitch = Math.abs(Rotations.getPitch(e2) - (double)MeteorClient.mc.player.getPitch());
         return Double.compare(Math.sqrt(e1yaw * e1yaw + e1pitch * e1pitch), Math.sqrt(e2yaw * e2yaw + e2pitch * e2pitch));
      }
   }

   private static int invertSort(int sort) {
      if (sort == 0) {
         return 0;
      } else {
         return sort > 0 ? -1 : 1;
      }
   }

   public static byte getSurroundBreak(LivingEntity target, BlockPos pos) {
      if (target.getPos().squaredDistanceTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5) > 16.0) {
         return 0;
      } else {
         BlockPos targetBlock = target.getBlockPos();
         PacketMine packetMine = (PacketMine)Modules.get().get(PacketMine.class);
         byte bestValue = 0;
         pos = pos.up();

         for(Box2 vec3i : CityUtils.CITY_WITHOUT_BURROW) {
            BlockPos offset = targetBlock.add(vec3i);
            if (MeteorClient.mc.world.getBlockState(offset).getBlock() != Blocks.BEDROCK
               && !BlockUtils2.outOfMiningRange(offset, Origin.NCP, 5.2)) {
               boolean x = vec3i.equals(CityUtils.CITY_WITHOUT_BURROW[0]) || vec3i.equals(CityUtils.CITY_WITHOUT_BURROW[2]);
               Box2 offsetVec = new Box2(x ? 0 : 1, 0, x ? 1 : 0);
               BlockPos multiTwo = offset.add(vec3i);
               byte value = 0;
               if (multiTwo.equals(pos)) {
                  value = 5;
               }

               if (multiTwo.add(offsetVec).equals(pos)) {
                  value = 4;
               }

               if (multiTwo.subtract(offsetVec).equals(pos)) {
                  value = 4;
               }

               BlockPos multiTwoDown = multiTwo.down();
               BlockPos offsetDown = offset.down();
               if (multiTwoDown.equals(pos)) {
                  value = 3;
               }

               if (multiTwoDown.add(offsetVec).equals(pos)) {
                  value = 2;
               }

               if (multiTwoDown.subtract(offsetVec).equals(pos)) {
                  value = 2;
               }

               if (offset.add(offsetVec).equals(pos)) {
                  value = 1;
               }

               if (offsetDown.add(offsetVec).equals(pos)) {
                  value = 1;
               }

               if (offset.subtract(offsetVec).equals(pos)) {
                  value = 1;
               }

               if (offsetDown.subtract(offsetVec).equals(pos)) {
                  value = 1;
               }

               if (offsetDown.equals(pos)) {
                  value = 1;
               }

               if (targetBlock.add(offsetVec).down().equals(pos)) {
                  value = 1;
               }

               if (targetBlock.subtract(offsetVec).down().equals(pos)) {
                  value = 1;
               }

               if (packetMine.isMineTarget(offset)) {
                  value = (byte)(value + 5);
               }

               if (value > bestValue) {
                  bestValue = value;
               }
            }
         }

         return bestValue;
      }
   }

   public static boolean isFucked(LivingEntity target) {
      int count = 0;
      int count2 = 0;
      if (isBurrowed(target)) {
         return false;
      } else {
         if (!MeteorClient.mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).getMaterial().isReplaceable()) {
            ++count;
         }

         if (!MeteorClient.mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).getMaterial().isReplaceable()) {
            ++count;
         }

         if (!MeteorClient.mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).getMaterial().isReplaceable()) {
            ++count;
         }

         if (!MeteorClient.mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).getMaterial().isReplaceable()) {
            ++count;
         }

         if (count == 4) {
            return false;
         } else if (count == 3) {
            return true;
         } else if (MeteorClient.mc.world.getBlockState(target.getBlockPos().add(0, 2, 0)).getBlock().collidable) {
            return true;
         } else {
            if (!MeteorClient.mc.world.getBlockState(target.getBlockPos().add(1, 1, 0)).getMaterial().isReplaceable()) {
               ++count2;
            }

            if (!MeteorClient.mc.world.getBlockState(target.getBlockPos().add(-1, 1, 0)).getMaterial().isReplaceable()) {
               ++count2;
            }

            if (!MeteorClient.mc.world.getBlockState(target.getBlockPos().add(0, 1, 1)).getMaterial().isReplaceable()) {
               ++count2;
            }

            if (!MeteorClient.mc.world.getBlockState(target.getBlockPos().add(0, 1, -1)).getMaterial().isReplaceable()) {
               ++count2;
            }

            return count2 == 4;
         }
      }
   }

   public static boolean isBurrowed(LivingEntity target) {
      return MeteorClient.mc
            .world
            .getBlockState(MUTABLE.set(target.getX() + 0.3, target.getY(), target.getZ() + 0.3))
            .getBlock()
            .collidable
         || MeteorClient.mc
            .world
            .getBlockState(MUTABLE.set(target.getX() + 0.3, target.getY(), target.getZ() - 0.3))
            .getBlock()
            .collidable
         || MeteorClient.mc
            .world
            .getBlockState(MUTABLE.set(target.getX() - 0.3, target.getY(), target.getZ() + 0.3))
            .getBlock()
            .collidable
         || MeteorClient.mc
            .world
            .getBlockState(MUTABLE.set(target.getX() - 0.3, target.getY(), target.getZ() - 0.3))
            .getBlock()
            .collidable;
   }

   public static boolean isObbyBurrowed(LivingEntity target) {
      return MeteorClient.mc
               .world
               .getBlockState(MUTABLE.set(target.getX() + 0.3, target.getY(), target.getZ() + 0.3))
               .getBlock()
               .getBlastResistance()
            > 600.0F
         || MeteorClient.mc
               .world
               .getBlockState(MUTABLE.set(target.getX() + 0.3, target.getY(), target.getZ() - 0.3))
               .getBlock()
               .getBlastResistance()
            > 600.0F
         || MeteorClient.mc
               .world
               .getBlockState(MUTABLE.set(target.getX() - 0.3, target.getY(), target.getZ() + 0.3))
               .getBlock()
               .getBlastResistance()
            > 600.0F
         || MeteorClient.mc
               .world
               .getBlockState(MUTABLE.set(target.getX() - 0.3, target.getY(), target.getZ() - 0.3))
               .getBlock()
               .getBlastResistance()
            > 600.0F;
   }

   public static Direction rayTraceCheck(BlockPos pos, boolean forceReturn) {
      Vec3d eyesPos = MeteorClient.mc.player.getEyePos();

      for(Direction direction : Direction.values()) {
         RaycastContext raycastContext = new RaycastContext(
            eyesPos, BlockUtils2.sideVec(pos, direction), class_3960.COLLIDER, class_242.NONE, MeteorClient.mc.player
         );
         BlockHitResult result = MeteorClient.mc.world.raycast(raycastContext);
         if (result != null && result.getType() == class_240.BLOCK && result.getBlockPos().equals(pos)) {
            return direction;
         }
      }

      if (!forceReturn) {
         return null;
      } else {
         return (double)pos.getY() > eyesPos.y ? Direction.DOWN : Direction.UP;
      }
   }

   public static boolean isSurroundBroken(LivingEntity target) {
      BlockPos targetBlockPos = target.getBlockPos();

      for(Box2 block : CityUtils.CITY_WITHOUT_BURROW) {
         if (isBlockSurroundBroken(targetBlockPos.add(block))) {
            return true;
         }
      }

      return false;
   }

   public static boolean isNaked(PlayerEntity player) {
      return (player.getInventory().armor == null || player.getInventory().armor.isEmpty())
         && (player.getOffHandStack() == null || player.getOffHandStack().isEmpty());
   }

   public static BlockHitResult getPlaceResult(BlockPos pos) {
      Vec3d eyesPos = new Vec3d(
         MeteorClient.mc.player.getX(),
         MeteorClient.mc.player.getY() + (double)MeteorClient.mc.player.getEyeHeight(MeteorClient.mc.player.getPose()),
         MeteorClient.mc.player.getZ()
      );

      for(Direction direction : Direction.values()) {
         RaycastContext raycastContext = new RaycastContext(
            eyesPos, BlockUtils2.sideVec(pos, direction), class_3960.COLLIDER, class_242.NONE, MeteorClient.mc.player
         );
         BlockHitResult result = MeteorClient.mc.world.raycast(raycastContext);
         if (result != null && result.getType() == class_240.BLOCK && result.getBlockPos().equals(pos)) {
            return result;
         }
      }

      return new BlockHitResult(Vec3d.ofCenter(pos), BlockUtils2.getClosestDirection(pos, false), pos, false);
   }

   public static boolean cantSee(BlockPos pos, boolean strictDirections) {
      Vec3d eyePos = PlayerUtils2.eyePos(MeteorClient.mc.player);
      int eyeY = (int)Math.ceil(eyePos.y);

      for(Direction direction : Direction.values()) {
         if (!strictDirections
            || AntiCheatHelper.isInteractableStrict(MeteorClient.mc.player.getBlockX(), eyeY, MeteorClient.mc.player.getBlockZ(), pos, direction)
            )
          {
            RaycastContext raycastContext = new RaycastContext(
               eyePos, BlockUtils2.sideVec(pos, direction), class_3960.COLLIDER, class_242.NONE, MeteorClient.mc.player
            );
            BlockHitResult result = MeteorClient.mc.world.raycast(raycastContext);
            if (result == null) {
               return true;
            } else {
               return switch(result.getType()) {
                  case BLOCK -> !result.getBlockPos().equals(pos);
                  case ENTITY, MISS -> false;
                  default -> throw new IncompatibleClassChangeError();
               };
            }
         }
      }

      return true;
   }

   public static boolean canSeeBlock(BlockPos hitBlock, Vec3d origin) {
      RaycastContext raycastContext = new RaycastContext(
         origin, Vec3d.ofCenter(hitBlock), class_3960.COLLIDER, class_242.NONE, MeteorClient.mc.player
      );
      BlockHitResult result = MeteorClient.mc.world.raycast(raycastContext);
      return result.getBlockPos().equals(hitBlock);
   }

   public static boolean isBlockSurroundBroken(BlockPos pos) {
      if (MeteorClient.mc.world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
         return false;
      } else {
         return BlockUtils2.outOfMiningRange(pos, Origin.NCP, 5.2) ? false : EntityUtils.intersectsWithEntity(new Box(pos), Entity::canHit);
      }
   }

   public static boolean isBlockSurroundBrokenByCrystal(BlockPos pos, EndCrystalEntity crystal) {
      return MeteorClient.mc.world.getBlockState(pos).isAir() && crystal.getBoundingBox().intersects(new Box(pos));
   }

   public static float getTotalHealth(LivingEntity target) {
      return target.getHealth() + target.getAbsorptionAmount();
   }

   public static boolean obbySurrounded(LivingEntity entity) {
      BlockPos pos = entity.getBlockPos();

      for(Box2 city : CityUtils.CITY_WITHOUT_BURROW) {
         if (!isBlastRes(MeteorClient.mc.world.getBlockState(pos.add(city)).getBlock())) {
            return false;
         }
      }

      return true;
   }

   public static boolean isBlastRes(Block block) {
      return block == Blocks.RESPAWN_ANCHOR && PlayerUtils.getDimension() == Dimension.Nether || block.getBlastResistance() >= 600.0F;
   }

   public static boolean isGoodForSurround(ItemStack stack) {
      Item item = stack.getItem();
      return item == Items.OBSIDIAN
         || item == Items.ANCIENT_DEBRIS
         || item == Items.CRYING_OBSIDIAN
         || item == Items.ANVIL
         || item == Items.CHIPPED_ANVIL
         || item == Items.DAMAGED_ANVIL
         || item == Items.ENCHANTING_TABLE
         || item == Items.ENDER_CHEST
         || item == Items.NETHERITE_BLOCK
         || PlayerUtils.getDimension() == Dimension.Nether && item == Items.RESPAWN_ANCHOR;
   }

   public static Vec3d smartVelocity(Entity entity) {
      return new Vec3d(entity.getX() - entity.prevX, entity.getY() - entity.prevY, entity.getZ() - entity.prevZ);
   }

   public static Vec3d smartPredictedPosition(Entity entity, Vec3d movement) {
      Box box = entity.getBoundingBox();
      List<VoxelShape> list = entity.world.getEntityCollisions(entity, box.stretch(movement));
      Vec3d vec3d = movement.lengthSquared() == 0.0 ? movement : adjustMovementForCollisions(entity, movement, box, MeteorClient.mc.world, list);
      boolean bl = movement.x != vec3d.x;
      boolean bl2 = movement.y != vec3d.y;
      boolean bl3 = movement.z != vec3d.z;
      boolean bl4 = entity.isOnGround() || bl2 && movement.y < 0.0;
      if (entity.stepHeight > 0.0F && bl4 && (bl || bl3)) {
         Vec3d vec3d2 = adjustMovementForCollisions(
            entity, new Vec3d(movement.x, (double)entity.stepHeight, movement.z), box, MeteorClient.mc.world, list
         );
         Vec3d vec3d3 = adjustMovementForCollisions(
            entity,
            new Vec3d(0.0, (double)entity.stepHeight, 0.0),
            box.stretch(movement.x, 0.0, movement.z),
            MeteorClient.mc.world,
            list
         );
         if (vec3d3.y < (double)entity.stepHeight) {
            Vec3d vec3d4 = adjustMovementForCollisions(
                  entity, new Vec3d(movement.x, 0.0, movement.z), box.offset(vec3d3), MeteorClient.mc.world, list
               )
               .add(vec3d3);
            if (vec3d4.horizontalLengthSquared() > vec3d2.horizontalLengthSquared()) {
               vec3d2 = vec3d4;
            }
         }

         if (vec3d2.horizontalLengthSquared() > vec3d.horizontalLengthSquared()) {
            return vec3d2.add(
               adjustMovementForCollisions(
                  entity, new Vec3d(0.0, -vec3d2.y + movement.y, 0.0), box.offset(vec3d2), MeteorClient.mc.world, list
               )
            );
         }
      }

      return vec3d;
   }

   private static Vec3d adjustMovementForCollisions(
      @Nullable Entity entity, Vec3d movement, Box entityBoundingBox, World world, List<VoxelShape> collisions
   ) {
      Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 1);
      if (!collisions.isEmpty()) {
         builder.addAll(collisions);
      }

      WorldBorder worldBorder = world.getWorldBorder();
      boolean bl = entity != null && worldBorder.canCollide(entity, entityBoundingBox.stretch(movement));
      if (bl) {
         builder.add(worldBorder.asVoxelShape());
      }

      builder.addAll(world.getBlockCollisions(entity, entityBoundingBox.stretch(movement)));
      return adjustMovementForCollisions(movement, entityBoundingBox, builder.build());
   }

   private static Vec3d adjustMovementForCollisions(Vec3d movement, Box entityBoundingBox, List<VoxelShape> collisions) {
      double d = movement.x;
      double e = movement.y;
      double f = movement.z;
      if (e != 0.0) {
         e = VoxelShapes.calculateMaxOffset(class_2351.Y, entityBoundingBox, collisions, e);
         if (e != 0.0) {
            entityBoundingBox = entityBoundingBox.offset(0.0, e, 0.0);
         }
      }

      boolean bl = Math.abs(d) < Math.abs(f);
      if (bl && f != 0.0) {
         f = VoxelShapes.calculateMaxOffset(class_2351.Z, entityBoundingBox, collisions, f);
         if (f != 0.0) {
            entityBoundingBox = entityBoundingBox.offset(0.0, 0.0, f);
         }
      }

      if (d != 0.0) {
         d = VoxelShapes.calculateMaxOffset(class_2351.X, entityBoundingBox, collisions, d);
         if (!bl && d != 0.0) {
            entityBoundingBox = entityBoundingBox.offset(d, 0.0, 0.0);
         }
      }

      if (!bl && f != 0.0) {
         f = VoxelShapes.calculateMaxOffset(class_2351.Z, entityBoundingBox, collisions, f);
      }

      return new Vec3d(d, e, f);
   }

   public static float yawFromDir(Direction direction) {
      return switch(direction) {
         case EAST -> -90.0F;
         case NORTH -> 180.0F;
         case WEST -> 90.0F;
         default -> 0.0F;
      };
   }

   public static Direction dirFromVec3d(Vec3d vec) {
      return Direction.fromRotation(Rotations.getYaw(vec));
   }

   public static boolean isSelfTrapBlock(LivingEntity target, BlockPos pos) {
      for(Box2 city : CITY_WITH_BURROW) {
         for(int i = 0; i < 3; ++i) {
            if (pos.equals(target.getBlockPos().add(city.up(i)))) {
               return true;
            }
         }
      }

      return false;
   }

   public static BlockPos[] playerBlocks(PlayerEntity player) {
      Box box = player.getBoundingBox();
      return new BlockPos[]{
         new BlockPos(box.maxX, box.minY, box.maxZ),
         new BlockPos(box.maxX, box.minY, box.minZ),
         new BlockPos(box.minX, box.minY, box.maxZ),
         new BlockPos(box.minX, box.minY, box.minZ),
         new BlockPos(box.maxX, box.maxY, box.maxZ),
         new BlockPos(box.maxX, box.maxY, box.minZ),
         new BlockPos(box.minX, box.maxY, box.maxZ),
         new BlockPos(box.minX, box.maxY, box.minZ)
      };
   }

   public static double getPlayerSpeed(boolean vertical) {
      double dX = MeteorClient.mc.player.getX() - MeteorClient.mc.player.prevX;
      double dY = MeteorClient.mc.player.getY() - MeteorClient.mc.player.prevY;
      double dZ = MeteorClient.mc.player.getZ() - MeteorClient.mc.player.prevZ;
      double distance = Math.sqrt(dX * dX + dZ * dZ + (vertical ? dY * dY : 0.0));
      distance *= ((Timer)Modules.get().get(Timer.class)).getMultiplier();
      return distance * 20.0;
   }
}
