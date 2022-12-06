package venomhack.utils;

import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import venomhack.enums.Origin;

public class AntiCheatHelper {
   public static boolean isInteractableStrict(int playerX, int playerY, int playerZ, BlockPos blockPos, Direction direction) {
      if (playerX == blockPos.getX() && playerY == blockPos.getY() && playerZ == blockPos.getZ()) {
         return true;
      } else {
         boolean fullBounds = MeteorClient.mc.world.getBlockState(blockPos).isFullCube(MeteorClient.mc.world, blockPos)
            || Statistics.get().pendingObsidian.containsKey(blockPos);
         Set<Direction> interactableDirections = getInteractableDirections(
            playerX - blockPos.getX(), playerY - blockPos.getY(), playerZ - blockPos.getZ(), fullBounds
         );
         if (!interactableDirections.contains(direction)) {
            return false;
         } else {
            return !isDirectionBlocked(blockPos, interactableDirections, direction, fullBounds);
         }
      }
   }

   public static Set<Direction> getInteractableDirections(int xdiff, int ydiff, int zdiff, boolean fullBounds) {
      HashSet<Direction> directions = new HashSet(6);
      if (!fullBounds) {
         if (xdiff == 0) {
            directions.add(Direction.EAST);
            directions.add(Direction.WEST);
         }

         if (zdiff == 0) {
            directions.add(Direction.SOUTH);
            directions.add(Direction.NORTH);
         }
      }

      if (ydiff == 0) {
         directions.add(Direction.UP);
         directions.add(Direction.DOWN);
      } else {
         directions.add(ydiff > 0 ? Direction.UP : Direction.DOWN);
      }

      if (xdiff != 0) {
         directions.add(xdiff > 0 ? Direction.EAST : Direction.WEST);
      }

      if (zdiff != 0) {
         directions.add(zdiff > 0 ? Direction.SOUTH : Direction.NORTH);
      }

      return directions;
   }

   public static boolean isDirectionBlocked(BlockPos block, Set<Direction> interactableDirections, Direction tDirection, boolean hasFullBounds) {
      BlockState offsetState = MeteorClient.mc.world.getBlockState(block.offset(tDirection));
      if (hasFullBounds) {
         return offsetState.getBlock().collidable && offsetState.isFullCube(MeteorClient.mc.world, block);
      } else {
         for(Direction direction : interactableDirections) {
            offsetState = MeteorClient.mc.world.getBlockState(block.offset(direction));
            if (!offsetState.isFullCube(MeteorClient.mc.world, block) || offsetState.getBlock().collidable) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean outOfHitRange(Entity target, Origin origin, double range, double wallsRange, boolean hitboxes) {
      double squaredDistanceVanilla = MeteorClient.mc.player.squaredDistanceTo(target.getPos());
      if (squaredDistanceVanilla >= 36.0) {
         return true;
      } else if (origin == Origin.VANILLA) {
         if (squaredDistanceVanilla >= range * range) {
            return true;
         } else {
            return squaredDistanceVanilla >= wallsRange * wallsRange && !MeteorClient.mc.player.canSee(target);
         }
      } else {
         if (MeteorClient.mc.interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
            range = 6.0;
         } else if (target instanceof EnderDragonEntity) {
            range += 6.5;
         } else if (target instanceof GiantEntity) {
            ++range;
         }

         Vec3d eyePos = PlayerUtils2.eyePos(MeteorClient.mc.player);
         double targetY = MathHelper.clamp(eyePos.y, target.getY(), target.getY() + (double)target.getHeight());
         double dx = target.getX() - eyePos.x;
         double dy = targetY - eyePos.y;
         double dz = target.getZ() - eyePos.z;
         double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (hitboxes && (eyePos.x != target.getX() || eyePos.z != target.getZ())) {
            double centerToEdge = 0.0;
            float targetHalfWidth = target.getWidth() * 0.5F;
            double hitX = target.getX();
            double hitZ = target.getZ();
            double xOffset = eyePos.x - target.getX();
            double zOffset = eyePos.z - target.getZ();
            double offsetLength = Math.sqrt(xOffset * xOffset + zOffset * zOffset);
            if (offsetLength >= (double)targetHalfWidth * Math.sqrt(2.0)) {
               if (zOffset > 0.0) {
                  hitZ = target.getZ() + (double)targetHalfWidth;
               } else if (zOffset < 0.0) {
                  hitZ = target.getZ() - (double)targetHalfWidth;
               } else if (xOffset > 0.0) {
                  hitX = target.getX() + (double)targetHalfWidth;
               } else {
                  hitX = target.getX() - (double)targetHalfWidth;
               }

               double adjustedHitVecLength = Math.sqrt(
                  (hitX - target.getX()) * (hitX - target.getX()) + (hitZ - target.getZ()) * (hitZ - target.getZ())
               );
               double dotProduct = xOffset * (hitX - target.getX()) + zOffset * (hitZ - target.getZ());
               double theta = Math.min(1.0, Math.max(dotProduct / (offsetLength * adjustedHitVecLength), -1.0));
               double angle = Math.acos(theta);
               if (angle > Math.PI / 4) {
                  angle = (Math.PI / 2) - angle;
               }

               if (angle >= 0.0 && angle <= Math.PI / 4) {
                  centerToEdge = (double)targetHalfWidth / Math.cos(angle);
               }
            }

            distance -= centerToEdge;
         }

         if (distance > range) {
            return true;
         } else {
            return distance > wallsRange && !MeteorClient.mc.player.canSee(target);
         }
      }
   }
}
