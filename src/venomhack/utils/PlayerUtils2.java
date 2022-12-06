package venomhack.utils;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.PlayerListEntry;
import org.jetbrains.annotations.Nullable;
import venomhack.mixinInterface.IBlockItem;

public class PlayerUtils2 {
   private static final Object2BooleanArrayMap<BlockPos> collisions = new Object2BooleanArrayMap();

   public static void init() {
      MeteorClient.EVENT_BUS.subscribe(PlayerUtils2.class);
   }

   @EventHandler
   private void onTick(Post event) {
      collisions.clear();
   }

   public static GameMode getGameMode(PlayerEntity player) {
      PlayerListEntry playerListEntry = MeteorClient.mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
      return playerListEntry == null ? GameMode.SPECTATOR : playerListEntry.getGameMode();
   }

   public static Vec3d predictPos(PlayerEntity player, int ticks, int antiStepOffset) {
      boolean hasStepped = false;
      Vec3d pos = player.getPos();
      Vec3d v = UtilsPlus.smartVelocity(player);
      Vec3d nextPos = pos;
      if (UtilsPlus.isSurrounded(player, true, true) && v.y <= 0.0) {
         return pos;
      } else {
         for(int i = 0; i <= ticks; ++i) {
            BlockPos newPos = new BlockPos(nextPos.add(v));
            if (collisionCheck(newPos) || Math.ceil((double)player.getEyeHeight(player.getPose())) != 1.0 && collisionCheck(newPos.up())) {
               if (antiStepOffset >= 1
                  && !hasStepped
                  && collisionCheck(newPos)
                  && !collisionCheck(newPos.up())
                  && !collisionCheck(newPos.up(2))) {
                  nextPos = nextPos.add(v).add(0.0, 1.0, 0.0);
                  hasStepped = true;
               } else if (antiStepOffset >= 2
                  && !hasStepped
                  && collisionCheck(newPos.up())
                  && !collisionCheck(newPos.up(2))
                  && !collisionCheck(newPos.up(3))) {
                  nextPos = nextPos.add(v).add(0.0, 2.0, 0.0);
                  hasStepped = true;
               }
            } else {
               nextPos = nextPos.add(v);
            }
         }

         return nextPos;
      }
   }

   public static Box predictBox(PlayerEntity player, int ticks, int antiStepOffset) {
      Vec3d nextPos = predictPos(player, ticks, antiStepOffset);
      Box oldBox = player.getBoundingBox();
      double dx = oldBox.getXLength() * 0.5;
      double dy = oldBox.getYLength();
      double dz = oldBox.getZLength() * 0.5;
      return new Box(
         nextPos.x - dx, nextPos.y, nextPos.z - dz, nextPos.x + dx, nextPos.y + dy, nextPos.z + dz
      );
   }

   private static boolean collisionCheck(BlockPos pos) {
      if (collisions.containsKey(pos)) {
         return collisions.getBoolean(pos);
      } else {
         boolean collides = MeteorClient.mc.world.getBlockState(pos).getBlock().collidable;
         collisions.put(pos, collides);
         return collides;
      }
   }

   public static Vec3d eyePos(PlayerEntity player) {
      return player.getPos().add(0.0, (double)player.getEyeHeight(player.getPose()), 0.0);
   }

   public static boolean canPlace(ItemPlacementContext context, BlockItem blockItem) {
      if (!context.canPlace()) {
         return false;
      } else {
         ItemPlacementContext itemPlacementContext = blockItem.getPlacementContext(context);
         if (itemPlacementContext == null) {
            return false;
         } else {
            BlockState blockState = ((IBlockItem)blockItem).getThePlacementState(itemPlacementContext);
            return blockState == null ? false : ((IBlockItem)blockItem).canBePlaced(itemPlacementContext, blockState);
         }
      }
   }

   public static void collectTargets(
      List<PlayerEntity> targets,
      @Nullable List<PlayerEntity> friends,
      int targetRange,
      int maxTargets,
      boolean ignoreNakeds,
      boolean onlyHoled,
      boolean ignoreTerrain,
      SortPriority sortPriority
   ) {
      ArrayList<LivingEntity> livings = new ArrayList();
      collectTargets(
         livings,
         friends,
         targetRange,
         maxTargets,
         ignoreNakeds,
         onlyHoled,
         ignoreTerrain,
         sortPriority,
         Utils.asO2BMap(new EntityType[]{EntityType.PLAYER})
      );
      targets.clear();

      for(LivingEntity living : livings) {
         targets.add((PlayerEntity)living);
      }
   }

   public static void collectTargets(
      List<LivingEntity> targets,
      @Nullable List<PlayerEntity> friends,
      int targetRange,
      int maxTargets,
      boolean ignoreNakeds,
      boolean onlyHoled,
      boolean ignoreTerrain,
      SortPriority sortPriority,
      Object2BooleanMap<EntityType<?>> entities
   ) {
      targets.clear();
      if (friends != null) {
         friends.clear();
      }

      for(Entity entity : MeteorClient.mc.world.getEntities()) {
         if (entity instanceof LivingEntity living
            && !living.isDead()
            && entity != MeteorClient.mc.player
            && !(MeteorClient.mc.player.distanceTo(entity) > (float)targetRange)) {
            if (entity instanceof PlayerEntity player) {
               if (EntityUtils.getGameMode(player) != GameMode.CREATIVE) {
                  if (Friends.get().shouldAttack(player)) {
                     if (entities.getBoolean(EntityType.PLAYER)
                        && (!ignoreNakeds || !UtilsPlus.isNaked(player))
                        && (!onlyHoled || UtilsPlus.isSurrounded(player, true, ignoreTerrain))) {
                        targets.add(player);
                     }
                  } else if (friends != null) {
                     friends.add(player);
                  }
               }
            } else if (entities.getBoolean(entity.getType())) {
               targets.add(living);
            }
         }
      }

      targets.sort((e1, e2) -> UtilsPlus.sort(e1, e2, sortPriority));

      while(targets.size() > maxTargets) {
         targets.remove(targets.size() - 1);
      }
   }

   public static ItemStack getStackFromResult(FindItemResult result) {
      if (MeteorClient.mc.player != null && !MeteorClient.mc.player.isDead()) {
         return result.isOffhand() ? MeteorClient.mc.player.getOffHandStack() : MeteorClient.mc.player.getInventory().getStack(result.slot());
      } else {
         return Items.AIR.getDefaultStack();
      }
   }

   public static Item getItemFromResult(FindItemResult result) {
      return getStackFromResult(result).getItem();
   }
}
