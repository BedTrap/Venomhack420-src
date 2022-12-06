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
import net.minecraft.util.Hand;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;
import venomhack.utils.UtilsPlus;

public class AutoWatercube extends ModuleHelper {
   private final Setting<AutoWatercube.Mode> mode = this.setting("mode", "Whether to place or to break.", AutoWatercube.Mode.Placing);
   private final Setting<AutoWatercube.PlaceMode> placemode = this.setting(
      "mode", "Full or partial for placing.", AutoWatercube.PlaceMode.Partial, () -> this.mode.get() == AutoWatercube.Mode.Placing
   );
   private final Setting<AutoWatercube.BreakMode> breakmode = this.setting(
      "mode", "Full or partial for breaking.", AutoWatercube.BreakMode.Packet, () -> this.mode.get() == AutoWatercube.Mode.Breaking
   );
   private final Setting<Double> range = this.setting("range", "The range in which to place/break kelp.", Double.valueOf(5.0), 0.0, 6.0);
   private final Setting<Boolean> rotate = this.setting("rotate", "Rotates towards the block when placing/breaking.", Boolean.valueOf(true));
   private final Setting<Integer> delay = this.setting("delay", "How many ticks to wait in between placing/breaking kelp.", Integer.valueOf(1));
   private final Setting<Integer> pertick = this.setting("kelp-per-tick", "How many kelp blocks to place/break per tick.", Integer.valueOf(5), 1.0, 10.0);
   private FindItemResult kelpresult;
   private int tickdelay;
   private Vec3d headCenterPos;

   public AutoWatercube() {
      super(Venomhack420.CATEGORY, "auto-watercube", "Automatically plants kelp in water.");
   }

   public void onActivate() {
      this.tickdelay = 0;
   }

   @EventHandler
   private void onTick(Pre event) {
      --this.tickdelay;
      this.kelpresult = InvUtils.findInHotbar(new Item[]{Items.KELP});
      if (this.kelpresult.slot() != -1 || this.mode.get() != AutoWatercube.Mode.Placing) {
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
                     if (this.mode.get() == AutoWatercube.Mode.Placing) {
                        if (!blockState.isFullCube(this.mc.world, blockPos)
                           && (!blockState.isOf(Blocks.KELP) || this.placemode.get() != AutoWatercube.PlaceMode.Full)) {
                           return;
                        }
   
                        if (!this.mc.world.getBlockState(blockPos.add(0, 1, 0)).isOf(Blocks.WATER)
                           || this.mc.world.getBlockState(blockPos.add(0, 1, 0)).getFluidState().getLevel() < 8) {
                           return;
                        }
   
                        if (positions.size() < this.pertick.get()) {
                           positions.add(new AtomicReference(blockPos.add(0, 1, 0).toImmutable()));
                        }
                     } else if (this.mode.get() == AutoWatercube.Mode.Breaking) {
                        if (!blockState.isOf(Blocks.KELP_PLANT) && !blockState.isOf(Blocks.KELP)) {
                           return;
                        }
   
                        if (positions.size() < this.pertick.get()) {
                           positions.add(new AtomicReference(blockPos.toImmutable()));
                        }
                     }
                  }
               }
            );
            BlockIterator.after(() -> {
               if (positions.size() > 0) {
                  for(AtomicReference<BlockPos> pos : positions) {
                     if (this.mode.get() == AutoWatercube.Mode.Placing) {
                        BlockUtils.place(((BlockPos)pos.get()).toImmutable(), this.kelpresult, this.rotate.get(), 50, false);
                     } else if (this.mode.get() == AutoWatercube.Mode.Breaking) {
                        if (this.breakmode.get() == AutoWatercube.BreakMode.Packet) {
                           UtilsPlus.mine((BlockPos)pos.get(), true, this.rotate.get());
                        } else if (this.breakmode.get() == AutoWatercube.BreakMode.Normal) {
                           this.mc.interactionManager.updateBlockBreakingProgress((BlockPos)pos.get(), Direction.UP);
                        }

                        this.mc.player.swingHand(Hand.MAIN_HAND);
                     }
                  }

                  this.tickdelay = this.delay.get();
               }
            });
         }
      }
   }

   public static enum BreakMode {
      Packet,
      Normal;
   }

   public static enum Mode {
      Placing,
      Breaking;
   }

   public static enum PlaceMode {
      Full,
      Partial;
   }
}
