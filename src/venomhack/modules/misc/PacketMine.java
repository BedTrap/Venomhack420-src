package venomhack.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.world.GameMode;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.network.Packet;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.class_2847;
import venomhack.Venomhack420;
import venomhack.enums.Origin;
import venomhack.modules.ModuleHelper;
import venomhack.modules.combat.AutoCrystal;
import venomhack.utils.BlockUtils2;
import venomhack.utils.ColorUtils;
import venomhack.utils.RandUtils;

public class PacketMine extends ModuleHelper {
   private final Setting<Boolean> strict = this.setting("strict", "For test 2b. might be buggy.", Boolean.valueOf(false));
   private final Setting<Double> range = this.setting("range", "How many blocks away you can mine.", Double.valueOf(5.2), this.sgGeneral, 0.0, 6.0);
   private final Setting<Origin> origin = this.setting("range-mode", "How to calculate the range.", Origin.NCP, this.sgGeneral);
   private final Setting<Boolean> rotate = this.setting("rotate", "Whether to rotate when mining.", Boolean.valueOf(false));
   private final Setting<Boolean> autoMine = this.setting("auto-remine", "Continues mining the block when it gets replaced.", Boolean.valueOf(true));
   private final Setting<Boolean> eatPause = this.setting(
      "pause-while-eating",
      "Won't swap slots while you are eating, to not interupt it.",
      Boolean.valueOf(true),
      this.sgGeneral,
      () -> this.autoMine.get() && !this.strict.get()
   );
   private final Setting<Integer> maxInstaMineAttempts = this.setting(
      "insta-mine-attempts",
      "How many times you want to attempt to insta mine in a row without having to remine.",
      Integer.valueOf(1),
      this.sgGeneral,
      this.autoMine::get
   );
   private final Setting<Boolean> swing = this.setting("swing", "Makes your hand swing client side when mining.", Boolean.valueOf(false));
   private final Setting<Boolean> render = this.setting("render", "Renders the block you are mining.", Boolean.valueOf(true));
   private final Setting<ShapeMode> shapeMode = this.setting("shape-mode", "How the shape is being rendered.", ShapeMode.Both, this.render::get);
   private final Setting<SettingColor> sideColor = this.setting("side-color", "The side color.", 255, 255, 255, 75, this.sgGeneral, this.render::get);
   private final Setting<SettingColor> lineColor = this.setting("line-color", "The line color.", 255, 255, 255, this.sgGeneral, this.render::get);
   private final Setting<Boolean> percColors = this.setting(
      "progress-colors", "Will render red when starting and green when finished with gradient.", Boolean.valueOf(false), this.sgGeneral, this.render::get
   );
   private float progress;
   private volatile boolean hasSwitched;
   private long breakTimeMs;
   private int bestSlot;
   private int amountOfInstaBreaks;
   private BlockState lastState;
   private BlockPos pos;
   private Direction direction;

   public PacketMine() {
      super(Venomhack420.CATEGORY, "packet-mine-vh", "Actual good packet mine. Can be used as auto mine.");
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.progress != -1.0F && this.pos != null && this.lastState != null) {
         if (BlockUtils2.outOfMiningRange(this.pos, (Origin)this.origin.get(), this.range.get())) {
            this.pos = null;
            this.lastState = null;
         } else {
            this.lastState = this.mc.world.getBlockState(this.pos);
            this.findBestSlot();
            boolean wasntFinished = this.progress < 1.0F;
            if (!this.lastState.isAir()) {
               this.progress = (float)(
                  (double)this.progress
                     + Math.max(0.0, this.getBreakDelta(this.bestSlot != -1 ? this.bestSlot : this.mc.player.getInventory().selectedSlot, this.lastState))
                        * 20.0
                        / (double)TickRate.INSTANCE.getTickRate()
               );
            }

            if (this.progress >= 1.0F) {
               if (this.lastState.isAir()) {
                  this.hasSwitched = false;
                  return;
               }

               if (!this.hasSwitched && this.amountOfInstaBreaks <= this.maxInstaMineAttempts.get()) {
                  if (this.canSwitch()) {
                     this.doBreakAndSwitch(this.pos, this.rotate.get());
                     if (wasntFinished) {
                        ((AutoCrystal)Modules.get().get(AutoCrystal.class)).doFacePlaceCity(this.pos);
                     }
                  }
               } else {
                  if ((double)TickRate.INSTANCE.getTimeSinceLastTick() > 1.5) {
                     return;
                  }

                  PlayerListEntry playerlistEntry = this.mc.player.networkHandler.getPlayerListEntry(this.mc.getSession().getProfile().getId());
                  if (playerlistEntry != null
                     && (float)(System.currentTimeMillis() - this.breakTimeMs)
                        > (float)MathHelper.clamp(playerlistEntry.getLatency(), 50, 300) * 20.0F / TickRate.INSTANCE.getTickRate()) {
                     if (this.autoMine.get()) {
                        if (this.amountOfInstaBreaks < this.maxInstaMineAttempts.get() && this.canSwitch()) {
                           this.doBreakAndSwitch(this.pos, this.rotate.get());
                        }

                        this.startMining(this.pos, this.rotate.get());
                     } else {
                        this.pos = null;
                        this.lastState = null;
                     }
                  }
               }
            }
         }
      }
   }

   private void doBreakAndSwitch(BlockPos pos, boolean rotate) {
      if (rotate) {
         Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 50, () -> this.doBreakAndSwitch(pos, false));
      } else {
         this.findBestSlot();
         if (this.bestSlot != -1) {
            this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.bestSlot));
         }

         this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.STOP_DESTROY_BLOCK, pos, this.direction));
         RandUtils.swing(this.swing.get());
         this.breakTimeMs = System.currentTimeMillis();
         this.hasSwitched = true;
         ++this.amountOfInstaBreaks;
         if (this.bestSlot != -1) {
            this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mc.player.getInventory().selectedSlot));
         }
      }
   }

   @EventHandler
   private void onStartMining(StartBreakingBlockEvent event) {
      PlayerListEntry playerlistEntry = this.mc.player.networkHandler.getPlayerListEntry(this.mc.getSession().getProfile().getId());
      if (playerlistEntry != null) {
         GameMode gamemode = playerlistEntry.getGameMode();
         if (gamemode == GameMode.SPECTATOR || gamemode == GameMode.ADVENTURE) {
            return;
         }
      }

      BlockState blockState = this.mc.world.getBlockState(event.blockPos);
      if (blockState.getHardness(null, null) <= 0.0F) {
         this.pos = null;
         this.lastState = null;
      } else {
         event.cancel();
         if (BlockUtils2.outOfMiningRange(event.blockPos, (Origin)this.origin.get(), this.range.get())) {
            this.pos = null;
         } else {
            this.direction = event.direction;
            if (!event.blockPos.equals(this.pos)) {
               this.pos = event.blockPos;
               this.lastState = blockState;
               this.startMining(this.pos, this.rotate.get());
            }
         }
      }
   }

   @EventHandler
   private void onPacketSend(Send event) {
      if (this.pos != null) {
         Packet var4 = event.packet;
         if (var4 instanceof PlayerActionC2SPacket packet) {
            if (packet.getPos().equals(this.pos) || this.mc.world.getBlockState(this.pos).isAir()) {
               return;
            }

            event.cancel();
         } else {
            var4 = event.packet;
            if (var4 instanceof UpdateSelectedSlotC2SPacket packet
               && this.strict.get()
               && this.bestSlot != -1
               && packet.getSelectedSlot() != this.bestSlot
               && !this.hasSwitched
               && this.lastState != null
               && !this.lastState.isAir()) {
               if (this.autoMine.get()) {
                  event.cancel();
               } else {
                  this.pos = null;
               }
            }
         }
      }
   }

   private void startMining(BlockPos pos, boolean rotate) {
      if (pos != null) {
         if (rotate) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 50, () -> this.startMining(pos, false));
         } else {
            if (this.strict.get()) {
               this.findBestSlot();
               if (this.bestSlot != -1) {
                  this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.bestSlot));
               }
            }

            this.hasSwitched = false;
            this.amountOfInstaBreaks = 0;
            this.progress = 0.0F;
            this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.START_DESTROY_BLOCK, pos, this.direction));
            this.mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(class_2847.ABORT_DESTROY_BLOCK, pos, this.direction));
            RandUtils.swing(this.swing.get());
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.pos != null && this.lastState != null && this.progress != -1.0F && this.render.get()) {
         float prog = 1.0F - MathHelper.clamp((this.progress > 0.5F ? this.progress - 0.5F : 0.5F - this.progress) * 2.0F, 0.0F, 1.0F);
         VoxelShape shape = this.lastState.getOutlineShape(this.mc.world, this.pos);
         if (!shape.isEmpty()) {
            Box original = shape.getBoundingBox();
            Box box = original.shrink(
               original.getXLength() * (double)prog, original.getYLength() * (double)prog, original.getZLength() * (double)prog
            );
            double xShrink = original.getXLength() * (double)prog * 0.5;
            double yShrink = original.getYLength() * (double)prog * 0.5;
            double zShrink = original.getZLength() * (double)prog * 0.5;
            Color sideProgressColor = ColorUtils.getColorFromPercent((double)this.progress).a(((SettingColor)this.sideColor.get()).a);
            Color lineProgressColor = ColorUtils.getColorFromPercent((double)this.progress).a(((SettingColor)this.lineColor.get()).a);
            event.renderer
               .box(
                  (double)this.pos.getX() + box.minX + xShrink,
                  (double)this.pos.getY() + box.minY + yShrink,
                  (double)this.pos.getZ() + box.minZ + zShrink,
                  (double)this.pos.getX() + box.maxX + xShrink,
                  (double)this.pos.getY() + box.maxY + yShrink,
                  (double)this.pos.getZ() + box.maxZ + zShrink,
                  this.percColors.get() ? sideProgressColor : (Color)this.sideColor.get(),
                  this.percColors.get() ? lineProgressColor : (Color)this.lineColor.get(),
                  (ShapeMode)this.shapeMode.get(),
                  0
               );
         }
      }
   }

   private boolean canSwitch() {
      if (this.eatPause.get() && this.bestSlot != -1) {
         return !this.mc.player.isUsingItem() || this.mc.player.getActiveHand() == Hand.OFF_HAND;
      } else {
         return true;
      }
   }

   public BlockState getState(BlockPos pos) {
      return this.isActive() && this.hasSwitched && this.progress >= 1.0F && pos.equals(this.pos)
         ? Blocks.AIR.getDefaultState()
         : this.mc.world.getBlockState(pos);
   }

   public boolean isMineTarget(BlockPos pos) {
      return this.isActive() && pos.equals(this.pos) && this.progress > 0.0F;
   }

   private void findBestSlot() {
      if (this.lastState != null) {
         this.bestSlot = -1;
         double bestScore = -1.0;

         for(int i = 0; i < 9; ++i) {
            ItemStack itemStack = this.mc.player.getInventory().getStack(i);
            float score = itemStack.getMiningSpeedMultiplier(this.lastState);
            if (score != 1.0F && (double)score > bestScore) {
               bestScore = (double)score;
               this.bestSlot = i;
            }
         }
      }
   }

   private double getBreakDelta(int slot, BlockState state) {
      PlayerListEntry playerlistEntry = this.mc.player.networkHandler.getPlayerListEntry(this.mc.getSession().getProfile().getId());
      if (playerlistEntry != null && playerlistEntry.getGameMode() == GameMode.CREATIVE) {
         return 1.0;
      } else {
         float hardness = state.getHardness(null, null);
         if (hardness == -1.0F) {
            return 0.0;
         } else {
            float speed = ((ItemStack)this.mc.player.getInventory().main.get(slot)).getMiningSpeedMultiplier(state);
            if (speed > 1.0F) {
               ItemStack tool = this.mc.player.getInventory().getStack(slot);
               int efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, tool);
               if (efficiency > 0 && !tool.isEmpty()) {
                  speed += (float)(efficiency * efficiency + 1);
               }
            }

            if (StatusEffectUtil.hasHaste(this.mc.player)) {
               speed *= 1.0F + (float)(StatusEffectUtil.getHasteAmplifier(this.mc.player) + 1) * 0.2F;
            }

            if (this.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
               speed *= (float)(this.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier() + 1) * 0.3F;
            }

            if (this.mc.player.isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(this.mc.player)) {
               speed /= 5.0F;
            }

            if (!this.mc.player.isOnGround()) {
               speed /= 5.0F;
            }

            return (double)(
               speed
                  / hardness
                  / (float)(state.isToolRequired() && !((ItemStack)this.mc.player.getInventory().main.get(slot)).isSuitableFor(state) ? 100 : 30)
            );
         }
      }
   }

   public void onDeactivate() {
      this.pos = null;
      this.lastState = null;
      this.progress = -1.0F;
      this.bestSlot = -1;
      if (this.mc.player != null) {
         this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.mc.player.getInventory().selectedSlot));
      }
   }
}
