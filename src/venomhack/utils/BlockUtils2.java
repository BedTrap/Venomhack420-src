package venomhack.utils;

import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Pair;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.class_2849;
import org.jetbrains.annotations.Nullable;
import venomhack.enums.Origin;

public class BlockUtils2 extends Utils {
   public static boolean placeBlock(
      FindItemResult result,
      BlockPos pos,
      boolean rotate,
      int rotationPriority,
      boolean airPlace,
      boolean ignoreEntity,
      boolean swing,
      boolean strictDirections
   ) {
      if (!result.found()) {
         return false;
      } else if (invalidPos(pos)) {
         return false;
      } else if (!MeteorClient.mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
         return false;
      } else {
         ItemStack itemStack = PlayerUtils2.getStackFromResult(result);
         BlockState placeState = Block.getBlockFromItem(itemStack.getItem()).getDefaultState();
         if (placeState.isAir()) {
            return false;
         } else if (!ignoreEntity && !MeteorClient.mc.world.canPlace(placeState, pos, ShapeContext.absent())) {
            return false;
         } else {
            BlockHitResult hitResult = getPlaceResult(pos, airPlace, strictDirections);
            if (hitResult == null) {
               return false;
            } else {
               justPlace(result, hitResult, swing, rotate, rotationPriority);
               if (placeState.getBlock() == Blocks.OBSIDIAN) {
                  Statistics.get().pendingObsidian.putIfAbsent(pos, System.currentTimeMillis());
               }

               return true;
            }
         }
      }
   }

   public static void justPlace(FindItemResult item, BlockHitResult hitResult, boolean swing, boolean rotate, int rotationPriority) {
      if (rotate) {
         Rotations.rotate(
            Rotations.getYaw(hitResult.getPos()),
            Rotations.getPitch(hitResult.getPos()),
            rotationPriority,
            () -> justPlace(item, hitResult, swing, false, 0)
         );
      } else {
         Hand hand = RandUtils.hand(item);
         boolean notSelected = !item.isOffhand() && MeteorClient.mc.player.getInventory().selectedSlot != item.slot();
         boolean sneak = !MeteorClient.mc.player.isSneaking()
            && clickableBlock(MeteorClient.mc.world.getBlockState(hitResult.getBlockPos()), hitResult, hand);
         if (sneak) {
            MeteorClient.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(MeteorClient.mc.player, class_2849.PRESS_SHIFT_KEY));
         }

         if (notSelected) {
            MeteorClient.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(item.slot()));
         }

         MeteorClient.mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
         RandUtils.swing(swing, hand);
         if (notSelected) {
            MeteorClient.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(MeteorClient.mc.player.getInventory().selectedSlot));
         }

         if (sneak) {
            MeteorClient.mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(MeteorClient.mc.player, class_2849.RELEASE_SHIFT_KEY));
         }
      }
   }

   public static boolean outOfMiningRange(BlockPos pos, Origin origin, double range) {
      if (origin == Origin.VANILLA) {
         double deltaX = MeteorClient.mc.player.getX() - ((double)pos.getX() + 0.5);
         double deltaY = MeteorClient.mc.player.getY() - ((double)pos.getY() + 0.5) + 1.5;
         double deltaZ = MeteorClient.mc.player.getZ() - ((double)pos.getZ() + 0.5);
         return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > range * range;
      } else {
         Vec3d eyesPos = PlayerUtils2.eyePos(MeteorClient.mc.player);
         double dx = eyesPos.x - (double)pos.getX() - 0.5;
         double dy = eyesPos.y - (double)pos.getY() - 0.5;
         double dz = eyesPos.z - (double)pos.getZ() - 0.5;
         return dx * dx + dy * dy + dz * dz > range * range;
      }
   }

   public static boolean outOfPlaceRange(BlockPos pos, Origin origin, double range) {
      if (origin == Origin.VANILLA) {
         return MeteorClient.mc.player.squaredDistanceTo((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5)
            >= range * range;
      } else {
         Vec3d eyesPos = PlayerUtils2.eyePos(MeteorClient.mc.player);
         double dx = eyesPos.x - (double)pos.getX() - 0.5;
         double dy = eyesPos.y - (double)pos.getY() - 0.5;
         double dz = eyesPos.z - (double)pos.getZ() - 0.5;
         return dx * dx + dy * dy + dz * dz > range * range;
      }
   }

   @Nullable
   public static BlockHitResult getPlaceResult(BlockPos block, boolean airPlace, boolean strictDirections) {
      return getPlaceResult(block, airPlace, strictDirections, null, null);
   }

   @Nullable
   public static BlockHitResult getPlaceResult(
      BlockPos block, boolean airPlace, boolean strictDirections, @Nullable ItemPlacementContext context, @Nullable BlockItem item
   ) {
      BlockPos neighbor = null;
      Direction placeSide = null;
      if (strictDirections) {
         Pair<BlockPos, Direction> pair = getStrictNeighbour(block);
         if (pair != null) {
            neighbor = (BlockPos)pair.getLeft();
            placeSide = (Direction)pair.getRight();
         }
      } else {
         placeSide = getClosestDirection(block, true);
         if (placeSide != null) {
            neighbor = block.offset(placeSide);
            placeSide = placeSide.getOpposite();
         }
      }

      if (neighbor == null || placeSide == null) {
         if (!airPlace) {
            return null;
         }

         neighbor = block;
         if (strictDirections) {
            placeSide = getStrictSide(block);
         } else {
            placeSide = getClosestDirection(block, false);
         }

         if (placeSide == null) {
            return null;
         }
      }

      Vec3d eyesPos = PlayerUtils2.eyePos(MeteorClient.mc.player);
      Vec3d vec = new Vec3d(
         MathHelper.clamp(eyesPos.x, (double)neighbor.getX(), (double)(neighbor.getX() + 1)),
         MathHelper.clamp(eyesPos.y, (double)neighbor.getY(), (double)(neighbor.getY() + 1)),
         MathHelper.clamp(eyesPos.z, (double)neighbor.getZ(), (double)(neighbor.getZ() + 1))
      );
      return new BlockHitResult(vec, placeSide, neighbor, false);
   }

   public static boolean placeNot(BlockPos pos, double minDamage, BlockState placeState) {
      VoxelShape shape = placeState.getCollisionShape(MeteorClient.mc.world, pos, ShapeContext.absent());
      return !MeteorClient.mc
         .world
         .getOtherEntities(
            null,
            shape.isEmpty() ? new Box(pos) : shape.getBoundingBox().offset(pos),
            entity -> entity.canHit()
                  && (
                     minDamage == 0.0
                        || !(entity instanceof EndCrystalEntity)
                        || (double)DamageCalcUtils.explosionDamage(MeteorClient.mc.player, entity.getPos(), 6) < minDamage
                  )
         )
         .isEmpty();
   }

   public static boolean invalidPos(BlockPos pos) {
      if (pos == null) {
         return true;
      } else {
         return MeteorClient.mc.world.isOutOfHeightLimit(pos.getY()) || !MeteorClient.mc.world.getWorldBorder().contains(pos);
      }
   }

   public static Vec3d getCenterPos(boolean hardSnap) {
      double x = MeteorClient.mc.player.getX() - (double)MeteorClient.mc.player.getBlockX();
      double z = MeteorClient.mc.player.getZ() - (double)MeteorClient.mc.player.getBlockZ();
      if (hardSnap) {
         x = 0.5;
         z = 0.5;
      } else {
         x = MathHelper.clamp(x, 0.31, 0.69);
         z = MathHelper.clamp(z, 0.31, 0.69);
      }

      return new Vec3d(
         (double)MeteorClient.mc.player.getBlockX() + x,
         MeteorClient.mc.player.getY(),
         (double)MeteorClient.mc.player.getBlockZ() + z
      );
   }

   public static void centerPlayer(boolean hardSnap) {
      MeteorClient.mc.player.setPosition(getCenterPos(hardSnap));
   }

   public static Set<BlockPos> getCity(LivingEntity player, boolean down, boolean check) {
      Box box = player.getBoundingBox().shrink(0.01, 0.01, 0.01);
      BlockPos playerBlockPos = player.getBlockPos();
      HashSet<BlockPos> cityBlocks = new HashSet();
      BlockPos minMin = playerBlockPos.add(
         box.minX - (double)playerBlockPos.getX(), 0.0, box.minZ - (double)playerBlockPos.getZ()
      );
      if (!check || MeteorClient.mc.world.getBlockState(minMin).getBlock().getHardness() >= 0.0F) {
         cityBlocks.add(minMin.add(Direction.WEST.getVector()));
         cityBlocks.add(minMin.add(Direction.NORTH.getVector()));
         if (down) {
            cityBlocks.add(minMin.add(Direction.DOWN.getVector()));
         }
      }

      BlockPos minMax = playerBlockPos.add(
         box.minX - (double)playerBlockPos.getX(), 0.0, box.maxZ - (double)playerBlockPos.getZ()
      );
      if (!check || MeteorClient.mc.world.getBlockState(minMax).getBlock().getHardness() >= 0.0F) {
         cityBlocks.add(minMax.add(Direction.WEST.getVector()));
         cityBlocks.add(minMax.add(Direction.SOUTH.getVector()));
         if (down) {
            cityBlocks.add(minMax.add(Direction.DOWN.getVector()));
         }
      }

      BlockPos maxMin = playerBlockPos.add(
         box.maxX - (double)playerBlockPos.getX(), 0.0, box.minZ - (double)playerBlockPos.getZ()
      );
      if (!check || MeteorClient.mc.world.getBlockState(maxMin).getBlock().getHardness() >= 0.0F) {
         cityBlocks.add(maxMin.add(Direction.EAST.getVector()));
         cityBlocks.add(maxMin.add(Direction.NORTH.getVector()));
         if (down) {
            cityBlocks.add(maxMin.add(Direction.DOWN.getVector()));
         }
      }

      BlockPos maxMax = playerBlockPos.add(
         box.maxX - (double)playerBlockPos.getX(), 0.0, box.maxZ - (double)playerBlockPos.getZ()
      );
      if (!check || MeteorClient.mc.world.getBlockState(maxMax).getBlock().getHardness() >= 0.0F) {
         cityBlocks.add(maxMax.add(Direction.EAST.getVector()));
         cityBlocks.add(maxMax.add(Direction.SOUTH.getVector()));
         if (down) {
            cityBlocks.add(maxMax.add(Direction.DOWN.getVector()));
         }
      }

      return cityBlocks;
   }

   @Nullable
   public static Pair<BlockPos, Direction> getStrictNeighbour(BlockPos pos) {
      BlockPos bestNeighbour = null;
      Direction bestDirection = null;
      double bestDistance = 420.0;
      Vec3d eyePos = PlayerUtils2.eyePos(MeteorClient.mc.player);
      int playerEyeY = (int)Math.floor(eyePos.y);

      for(Direction direction : Direction.values()) {
         BlockPos neighbour = pos.offset(direction);
         if (!MeteorClient.mc.world.getBlockState(neighbour).getMaterial().isReplaceable() || Statistics.get().pendingObsidian.containsKey(neighbour)) {
            Direction opposite = direction.getOpposite();
            if (AntiCheatHelper.isInteractableStrict(
               MeteorClient.mc.player.getBlockX(), playerEyeY, MeteorClient.mc.player.getBlockZ(), neighbour, opposite
            )) {
               double distance = eyePos.squaredDistanceTo(sideVec(neighbour, opposite));
               if (distance < bestDistance) {
                  bestNeighbour = neighbour;
                  bestDirection = opposite;
                  bestDistance = distance;
               }
            }
         }
      }

      return bestNeighbour != null ? new Pair(bestNeighbour, bestDirection) : null;
   }

   @Nullable
   public static Direction getStrictSide(BlockPos pos) {
      Direction bestDirection = null;
      double bestDistance = 420.0;
      Vec3d eyePos = PlayerUtils2.eyePos(MeteorClient.mc.player);
      int playerEyeY = (int)Math.floor(eyePos.y);

      for(Direction direction : Direction.values()) {
         if (AntiCheatHelper.isInteractableStrict(
            MeteorClient.mc.player.getBlockX(), playerEyeY, MeteorClient.mc.player.getBlockZ(), pos, direction
         )) {
            if (direction == Direction.DOWN) {
               return direction;
            }

            double distance = eyePos.squaredDistanceTo(sideVec(pos, direction));
            if (distance < bestDistance) {
               bestDirection = direction;
               bestDistance = distance;
            }
         }
      }

      return bestDirection;
   }

   public static Vec3d sideVec(BlockPos pos, Direction direction) {
      return Vec3d.ofCenter(pos)
         .add((double)direction.getOffsetX() * 0.5, (double)direction.getOffsetY() * 0.5, (double)direction.getOffsetZ() * 0.5);
   }

   public static Direction getClosestDirection(BlockPos pos, boolean withSupport) {
      Direction bestDirection = null;
      double bestDistance = 69.0;

      for(Direction direction : Direction.values()) {
         BlockPos neighbour = pos.offset(direction);
         if (!withSupport
            || !MeteorClient.mc.world.getBlockState(neighbour).getMaterial().isReplaceable()
            || Statistics.get().pendingObsidian.containsKey(neighbour)) {
            if (direction == Direction.DOWN) {
               return direction;
            }

            double distance = PlayerUtils2.eyePos(MeteorClient.mc.player).distanceTo(sideVec(pos, direction));
            if (distance < bestDistance) {
               bestDistance = distance;
               bestDirection = direction;
            }
         }
      }

      return bestDirection;
   }

   public static boolean noSupport(BlockPos pos) {
      for(Direction side : Direction.values()) {
         BlockPos neighbor = pos.offset(side);
         if (!MeteorClient.mc.world.getBlockState(neighbor).getMaterial().isReplaceable()) {
            return false;
         }
      }

      return true;
   }

   public static boolean clickableBlock(BlockState state, BlockHitResult hit, Hand hand) {
      return state.onUse(MeteorClient.mc.world, MeteorClient.mc.player, hand, hit) != ActionResult.PASS;
   }
}
