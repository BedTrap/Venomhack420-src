package venomhack.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.util.Hand;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

public class RandUtils {
   public static int countEmptySlots() {
      int emptySlots = 0;

      for(int i = 0; i < 36; ++i) {
         ItemStack stack = MeteorClient.mc.player.getInventory().getStack(i);
         if (stack.isEmpty()) {
            ++emptySlots;
         }
      }

      return emptySlots;
   }

   public static Direction direction(Vec3i vec3i) {
      return Direction.fromVector(vec3i.getX(), vec3i.getY(), vec3i.getZ());
   }

   public static void rotate(BlockPos blockPos, Runnable callback) {
      rotate(Vec3d.ofCenter(blockPos), callback);
   }

   public static void rotate(Vec3d vec3d, Runnable callback) {
      Rotations.rotate(Rotations.getYaw(vec3d), Rotations.getPitch(vec3d), callback);
   }

   public static Hand hand(FindItemResult result) {
      return result.getHand() == null ? Hand.MAIN_HAND : result.getHand();
   }

   public static void swing(boolean clientSide) {
      swing(clientSide, Hand.MAIN_HAND);
   }

   public static void swing(boolean clientSide, Hand hand) {
      if (clientSide) {
         MeteorClient.mc.player.swingHand(hand);
      } else {
         MeteorClient.mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
      }
   }

   public static int bounds(double value) {
      if (value == 0.0) {
         return 0;
      } else {
         return value < 0.0 ? -1 : 1;
      }
   }

   public static double horizontalDistance(Vec3d vec1, Vec3d vec2) {
      double dx = vec1.x - vec2.x;
      double dz = vec1.z - vec2.z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   public static double durabilityPercentage(ItemStack stack) {
      return 100.0 * (double)(stack.getMaxDamage() - stack.getDamage()) / (double)stack.getMaxDamage();
   }

   public static void clickSlotPacket(int fromIndex, int toIndex, SlotActionType type) {
      ScreenHandler sh = MeteorClient.mc.player.currentScreenHandler;
      Slot slot = sh.getSlot(fromIndex);
      Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap();
      stack.put(fromIndex, slot.getStack());
      MeteorClient.mc
         .player
         .networkHandler
         .sendPacket(new ClickSlotC2SPacket(sh.syncId, sh.getRevision(), slot.id, toIndex, type, sh.getSlot(fromIndex).getStack(), stack));
   }
}
