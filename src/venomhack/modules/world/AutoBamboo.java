package venomhack.modules.world;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.SandBlock;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class AutoBamboo extends ModuleHelper {
   private final Setting<Double> range = this.setting("range", "The range in which to place bamboo.", Double.valueOf(5.0), 0.0, 6.0);
   private final Setting<Boolean> rotate = this.setting("rotate", "Rotates towards the block when placing.", Boolean.valueOf(true));
   private final Setting<Integer> clearance = this.setting(
      "clearance", "How many blocks to check for air above the sand (higher values may negatively affect performance).", Integer.valueOf(1), 1.0, 16.0
   );
   private final Setting<Integer> delay = this.setting("delay", "How many ticks to wait in between placing bamboo.", Integer.valueOf(1));
   private final Setting<Integer> pertick = this.setting("bamboo-per-tick", "How many bamboo blocks to place per tick.", Integer.valueOf(5), 1.0, 10.0);
   private FindItemResult bambooResult;
   private int tickdelay;
   private Vec3d headCenterPos;

   public AutoBamboo() {
      super(Venomhack420.CATEGORY, "auto-bamboo", "Automatically plants bamboo.");
   }

   public void onActivate() {
      this.tickdelay = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      --this.tickdelay;
      this.bambooResult = InvUtils.findInHotbar(new Item[]{Items.BAMBOO});
      if (this.bambooResult.slot() != -1) {
         this.headCenterPos = this.mc
            .player
            .getPos()
            .add(0.0, (double)this.mc.player.getEyeHeight(this.mc.player.getPose()), 0.0);
         if (this.tickdelay <= 0) {
            ArrayList<AtomicReference<BlockPos>> positions = new ArrayList<>();
            BlockIterator.register(
               (int)Math.ceil(this.range.get()),
               (int)Math.ceil(this.range.get()),
               (blockPos, blockState) -> {
                  Vec3d vecpos = new Vec3d(
                     (double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5
                  );
                  if (!(this.headCenterPos.distanceTo(vecpos) > this.range.get())) {
                     if (blockState.getBlock() instanceof SandBlock
                        || blockState.isOf(Blocks.GRAVEL)
                        || blockState.isOf(Blocks.DIRT)
                        || blockState.isOf(Blocks.COARSE_DIRT)
                        || blockState.isOf(Blocks.GRASS_BLOCK)) {
                        boolean isclear = true;
   
                        for(int i = 1; i <= this.clearance.get(); ++i) {
                           if (!this.mc.world.getBlockState(blockPos.add(0, i, 0)).isAir()) {
                              isclear = false;
                           }
                        }
   
                        if (isclear) {
                           if (positions.size() < this.pertick.get()) {
                              positions.add(new AtomicReference(blockPos.add(0, 1, 0).toImmutable()));
                           }
                        }
                     }
                  }
               }
            );
            BlockIterator.after(() -> {
               if (positions.size() > 0) {
                  for(AtomicReference<BlockPos> pos : positions) {
                     BlockUtils.place(((BlockPos)pos.get()).toImmutable(), this.bambooResult, this.rotate.get(), 50, true);
                     System.out.println(pos);
                  }

                  this.tickdelay = this.delay.get();
               }
            });
         }
      }
   }
}
