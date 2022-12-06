package venomhack.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.tag.FluidTags;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.class_2847;
import venomhack.modules.combat.AutoCity;

public class CityUtils extends Utils {
   public static Vec3i[] CITY_WITHOUT_BURROW = new Vec3i[]{
      new Vec3i(1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 0), new Vec3i(0, 0, -1)
   };

   public static int getSpeed(BlockState state, BlockPos pos) {
      FindItemResult result = InvUtils.findFastestTool(state);
      return getBlockBreakingSpeed(state, pos, result.found() ? result.slot() : MeteorClient.mc.player.getInventory().selectedSlot);
   }

   public static int getBlockBreakingSpeed(BlockState block, BlockPos pos, int slot) {
      ClientPlayerEntity player = MeteorClient.mc.player;
      float f = player.getInventory().getStack(slot).getMiningSpeedMultiplier(block);
      if (f > 1.0F) {
         int i = EnchantmentHelper.get(player.getInventory().getStack(slot)).getOrDefault(Enchantments.EFFICIENCY, 0);
         if (i > 0) {
            f += (float)(i * i + 1);
         }
      }

      if (StatusEffectUtil.hasHaste(player)) {
         f *= 1.0F + (float)(StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
      }

      if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
         float k = switch(player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
            case 0 -> 0.3F;
            case 1 -> 0.09F;
            case 2 -> 0.0027F;
            default -> 8.1E-4F;
         };
         f *= k;
      }

      if (player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(player)) {
         f /= 5.0F;
      }

      if (!player.isOnGround()) {
         f /= 5.0F;
      }

      float t = block.getHardness(MeteorClient.mc.world, pos);
      return t == -1.0F ? 0 : (int)Math.ceil((double)(1.0F / (f / t / 30.0F)));
   }

   public static void mine(BlockPos blockPos, boolean swing, boolean rotate) {
      if (rotate) {
         Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> mine(blockPos, swing, false));
      } else {
         MeteorClient.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(class_2847.START_DESTROY_BLOCK, blockPos, Direction.UP));
         if (swing) {
            MeteorClient.mc.player.swingHand(Hand.MAIN_HAND);
         } else {
            MeteorClient.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }

         MeteorClient.mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(class_2847.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
      }
   }

   public static BlockPos getCity(PlayerEntity player, double range, boolean mineBurrow, boolean ignoreSelfWebs) {
      BlockPos targetPos = player.getBlockPos();
      if (mineBurrow && isGood(targetPos, range, ignoreSelfWebs)) {
         return targetPos;
      } else {
         List<BlockPos> posList = new ArrayList();
         List<BlockPos> brokenList = new ArrayList();

         for(Vec3i cities : CITY_WITHOUT_BURROW) {
            BlockPos citied = targetPos.add(cities);
            if (isGood(citied, range, false)
               && (
                  !((AutoCity)Modules.get().get(AutoCity.class)).old.get()
                     || MeteorClient.mc.world.getBlockState(citied.add(0, 1, 0)).isAir()
               )) {
               posList.add(citied);
               if (UtilsPlus.isBlockSurroundBroken(citied)) {
                  brokenList.add(citied);
               }
            }
         }

         posList.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
         brokenList.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
         return !brokenList.isEmpty() ? (BlockPos)brokenList.get(0) : (posList.isEmpty() ? null : (BlockPos)posList.get(0));
      }
   }

   private static boolean isGood(BlockPos pos, double range, boolean websBad) {
      BlockState state = MeteorClient.mc.world.getBlockState(pos);
      if (websBad && state.isOf(Blocks.COBWEB)) {
         return false;
      } else {
         for(Vec3i cities : CITY_WITHOUT_BURROW) {
            if (MeteorClient.mc.player.getBlockPos().add(cities).equals(pos)) {
               return false;
            }
         }

         return !state.isAir()
            && !state.isOf(Blocks.BEDROCK)
            && pos.isWithinDistance(
               MeteorClient.mc
                  .player
                  .getBlockPos()
                  .add(0.0, (double)MeteorClient.mc.player.getEyeHeight(MeteorClient.mc.player.getPose()), 0.0),
               range
            );
      }
   }
}
