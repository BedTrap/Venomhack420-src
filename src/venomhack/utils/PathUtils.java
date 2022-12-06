package venomhack.utils;

import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import org.jetbrains.annotations.Nullable;

public class PathUtils extends Utils {
   private int maxLength;
   private BlockState placeState;

   public PathUtils(int maxLength, BlockState placeState) {
      this.maxLength = maxLength;
      this.placeState = placeState;
   }

   public void setPlaceState(BlockState placeState) {
      this.placeState = placeState;
   }

   public void setMaxLength(int maxLength) {
      this.maxLength = maxLength;
   }

   public static void smartAdd(Set<BlockPos> set, BlockPos pos) {
      if (!set.contains(pos)) {
         set.add(pos);
      }
   }

   public static void smartAdd(List<BlockPos> list, BlockPos pos) {
      if (!list.contains(pos)) {
         list.add(pos);
      }
   }

   public static void smartAdd(List<PathUtils.PathBlock> list, PathUtils.PathBlock pos) {
      if (!list.contains(pos)) {
         list.add(pos);
      }
   }

   private PathUtils.PathResult findPath(
      List<PathUtils.PathBlock> list, PathUtils.PathBlock pos, int iteration, @Nullable List<PathUtils.PathBlock> shortestPath
   ) {
      if (iteration <= this.maxLength && (shortestPath == null || iteration <= shortestPath.size())) {
         for(Direction direction : Direction.values()) {
            if (MeteorClient.mc.world.canPlace(this.placeState, pos.offset(direction).getBlockPos(), ShapeContext.absent())) {
               if (list.contains(pos.offset(direction))) {
                  return new PathUtils.PathResult(list, pos.setPredictDirection(direction.getOpposite()));
               }

               if (!MeteorClient.mc.world.getBlockState(pos.offset(direction).getBlockPos()).getMaterial().isReplaceable()) {
                  return new PathUtils.PathResult(list, pos);
               }
            }
         }

         for(Direction direction : Direction.values()) {
            if (MeteorClient.mc.world.canPlace(this.placeState, pos.offset(direction).getBlockPos(), ShapeContext.absent())) {
               PathUtils.PathResult pathResult = this.findPath(list, pos.offset(direction), iteration + 1, null);
               if (pathResult != null && !pathResult.list.isEmpty()) {
                  smartAdd(list, pos);
                  return new PathUtils.PathResult(list, pos);
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static class PathBlock {
      private final BlockPos pos;
      private final int priority;
      private Direction predictDirection = null;

      public PathBlock(BlockPos pos, int priority) {
         this.pos = pos;
         this.priority = priority;
      }

      public PathBlock(BlockPos pos, int priority, Direction predictDirection) {
         this.pos = pos;
         this.priority = priority;
         this.predictDirection = predictDirection;
      }

      public PathUtils.PathBlock offset(Direction direction) {
         return new PathUtils.PathBlock(this.pos.offset(direction), this.priority);
      }

      public BlockPos getBlockPos() {
         return this.pos;
      }

      public int getPriority() {
         return this.priority;
      }

      public PathUtils.PathBlock setPredictDirection(Direction predictDirection) {
         return new PathUtils.PathBlock(this.pos, this.priority, predictDirection);
      }

      public Direction getPredictDirection() {
         return this.predictDirection;
      }
   }

   public static class PathResult {
      private final List<PathUtils.PathBlock> list;
      private final PathUtils.PathBlock pos;

      public PathResult(List<PathUtils.PathBlock> list, PathUtils.PathBlock pos) {
         this.list = list;
         this.pos = pos;
      }

      public List<PathUtils.PathBlock> getList() {
         return this.list;
      }

      public PathUtils.PathBlock getPos() {
         return this.pos;
      }
   }
}
