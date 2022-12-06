package venomhack.modules.combat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import venomhack.Venomhack420;
import venomhack.enums.Origin;
import venomhack.enums.RenderShape;
import venomhack.modules.ModuleHelper;
import venomhack.utils.BlockUtils2;
import venomhack.utils.PlayerUtils2;
import venomhack.utils.UtilsPlus;
import venomhack.utils.customObjects.RenderBlock;

public class HoleFill extends ModuleHelper {
   private final SettingGroup sgSmart = this.group("Smart");
   private final SettingGroup sgSafety = this.group("Safety");
   private final SettingGroup sgRender = this.group("Render");
   private final Setting<List<Block>> blocks = this.setting(
      "blocks", "Which blocks can be used to fill holes.", this.sgGeneral, null, new Block[]{Blocks.OBSIDIAN, Blocks.COBWEB}
   );
   private final Setting<Origin> placeOrigin = this.setting("place-origin", "How to calculate ranges.", Origin.NCP);
   private final Setting<Double> placeRange = this.setting("place-range", "How far you can place blocks.", Double.valueOf(5.2), 0.0, 6.0);
   private final Setting<Double> wallsRange = this.setting("walls-range", "How far you can place through walls.", Double.valueOf(5.2), 0.0, 6.0);
   private final Setting<Boolean> pauseEating = this.setting(
      "pause-while-eating", "Will only attempt to fill holes while you aren't eating.", Boolean.valueOf(true)
   );
   private final Setting<Boolean> doubles = this.setting("doubles", "Fills double holes.", Boolean.valueOf(true));
   private final Setting<Integer> placeDelay = this.setting("delay", "The delay between placements in ticks.", Integer.valueOf(1));
   private final Setting<Integer> blocksPerTick = this.setting("blocks-per-tick", "How many blocks to place per tick.", Integer.valueOf(5));
   private final Setting<Boolean> strictDirections = this.setting("strict-directions", "Places only on visible sides.", Boolean.valueOf(false));
   private final Setting<Boolean> rotate = this.setting("rotate", "Automatically rotates towards to where you are filling.", Boolean.valueOf(false));
   private final Setting<Boolean> smart = this.setting("smart", "Only fills holes within a certain range of a target.", Boolean.valueOf(true), this.sgSmart);
   private final Setting<Boolean> onlyOnCa = this.setting(
      "only-on-ca", "Will only fill holes when Crystal Aura or Auto Crystal is turned on.", Boolean.valueOf(true), this.sgSmart, this.smart::get
   );
   private final Setting<Double> smartRadiusH = this.setting(
      "horizontal-radius-smart", "Horizontal radius from a target in which to fill holes.", Double.valueOf(2.0), this.sgSmart, this.smart::get, 0.0, 5.0
   );
   private final Setting<Double> smartRadiusV = this.setting(
      "vertical-radius-smart", "Vertical radius from a target in which to fill holes.", Double.valueOf(4.0), this.sgSmart, this.smart::get, 0.0, 5.0
   );
   private final Setting<Boolean> onlyMoving = this.setting(
      "only-moving", "Will only fill holes around moving targets.", Boolean.valueOf(true), this.sgSmart, this.smart::get
   );
   private final Setting<Keybind> forceFill = this.setting(
      "force-fill", "Will fill all holes around you when pressed.", Keybind.fromKey(-1), this.sgSmart, this.smart::get
   );
   private final Setting<Boolean> predict = this.setting(
      "predict-movement",
      "Will add the target's velocity to its position times the amount of predict ticks.",
      Boolean.valueOf(false),
      this.sgSmart,
      this.smart::get
   );
   private final Setting<Integer> predictTicks = this.setting(
      "predict-ticks", "How many ticks to predict the movement for.", Integer.valueOf(2), this.sgSmart, () -> this.smart.get() && this.predict.get()
   );
   private final Setting<Boolean> onlySafe = this.setting(
      "only-when-safe", "Will only fill holes when you are surrounded or burrowed.", Boolean.valueOf(false), this.sgSafety
   );
   private final Setting<Double> safeRadiusH = this.setting(
      "min-horizontal-distance", "Horizontal radius from yourself in which to fill holes.", Double.valueOf(0.0), this.sgSafety, 0.0, 5.0
   );
   private final Setting<Double> safeRadiusV = this.setting(
      "min-vertical-distance", "Vertical radius from yourself in which to fill holes.", Double.valueOf(0.0), this.sgSafety, 0.0, 5.0
   );
   private final Setting<Boolean> swing = this.setting("swing", "Renders your hand swing client-side.", Boolean.valueOf(true), this.sgRender);
   private final Setting<Boolean> render = this.setting("render", "Creates a render effect where you are placing.", Boolean.valueOf(true), this.sgRender);
   private final Setting<RenderShape> renderShape = this.setting("render-shape", "What shape mode to use.", RenderShape.CORNER_PYRAMIDS, this.sgRender);
   private final Setting<ShapeMode> shapeMode = this.setting(
      "shape-mode", "What part of the shapes to render.", ShapeMode.Both, this.sgRender, this.render::get
   );
   private final Setting<Double> height = this.setting("height", "The height of the render.", Double.valueOf(1.0), this.sgRender, this.render::get, 0.0, 2.0);
   private final Setting<Double> width = this.setting("width", "The width of the render.", Double.valueOf(1.0), this.sgRender, this.render::get, 0.0, 2.0);
   private final Setting<Double> yOffset = this.setting(
      "y-offset", "The height shift for the rendered shape.", Double.valueOf(0.0), this.sgRender, this.render::get, 0.0, 1.0
   );
   private final Setting<Double> weirdOffset = this.setting(
      "weird-offset",
      "Offset for the pyramid render shape.",
      Double.valueOf(0.0),
      this.sgRender,
      () -> this.render.get() && this.renderShape.get() == RenderShape.CORNER_PYRAMIDS,
      -1.0,
      1.0
   );
   private final Setting<Integer> renderTime = this.setting(
      "render-time", "For how long a shape should be rendered.", Integer.valueOf(10), this.sgRender, this.render::get
   );
   private final Setting<Boolean> fade = this.setting(
      "fade", "Will reduce the opacity of the rendered block over time.", Boolean.valueOf(true), this.sgRender, this.render::get
   );
   private final Setting<SettingColor> sideColor = this.setting(
      "side-color", "The side color of the target block rendering.", 197, 137, 232, 10, this.sgRender
   );
   private final Setting<SettingColor> lineColor = this.setting("line-color", "The line color of the target block rendering.", 197, 137, 232, this.sgRender);
   private final Set<BlockPos> holes = new HashSet();
   private final List<RenderBlock> renderBlocks = new ArrayList<>();
   private int delayLeft;

   public HoleFill() {
      super(Venomhack420.CATEGORY, "hole-fill", "Fills holes with specified blocks.");
   }

   public void onActivate() {
      this.delayLeft = 0;
      this.renderBlocks.clear();
      this.holes.clear();
   }

   @EventHandler
   private void onTick(Pre event) {
      RenderBlock.tick(this.renderBlocks);
      if (this.delayLeft > 0) {
         --this.delayLeft;
      } else if (!this.pauseEating.get() || !PlayerUtils.shouldPause(false, this.pauseEating.get(), false)) {
         if (!this.onlySafe.get() || UtilsPlus.isSafe(this.mc.player)) {
            FindItemResult result = InvUtils.findInHotbar(
               itemStack -> itemStack.getItem() instanceof BlockItem
                     && ((List)this.blocks.get()).contains(Block.getBlockFromItem(itemStack.getItem()))
            );
            if (result.found()) {
               this.holes.clear();
               BlockIterator.register(
                  (int)Math.ceil(this.placeRange.get() + 1.0),
                  (int)Math.ceil(this.placeRange.get() + 1.0),
                  (blockPos, blockState) -> {
                     int count = 0;
                     Direction doubleHoleOffset = null;
   
                     for(Direction direction : Direction.values()) {
                        if (direction != Direction.UP) {
                           BlockState state = this.mc.world.getBlockState(blockPos.offset(direction));
                           if (state.getBlock().getBlastResistance() >= 600.0F) {
                              ++count;
                           } else {
                              if (direction == Direction.DOWN) {
                                 return;
                              }
   
                              if (this.doubles.get()
                                 && doubleHoleOffset == null
                                 && this.validHole(blockPos.offset(direction), null, this.getBlock(result))) {
                                 for(Direction dir : Direction.values()) {
                                    if (dir != direction.getOpposite() && dir != Direction.UP) {
                                       BlockState blockState1 = this.mc.world.getBlockState(blockPos.offset(direction).offset(dir));
                                       if (!(blockState1.getBlock().getBlastResistance() >= 600.0F)) {
                                          return;
                                       }
   
                                       ++count;
                                    }
                                 }
   
                                 doubleHoleOffset = direction;
                              }
                           }
                        }
                     }
   
                     if (this.validHole(blockPos, doubleHoleOffset, this.getBlock(result))) {
                        if (count == 5 && doubleHoleOffset == null) {
                           this.holes.add(new BlockPos(blockPos));
                        } else if (count == 8 && doubleHoleOffset != null) {
                           this.holes.add(blockPos);
                           this.holes.add(blockPos.offset(doubleHoleOffset));
                        }
                     }
                  }
               );
               BlockIterator.after(() -> {
                  int blocksPlaced = 0;

                  for(BlockPos holePos : this.holes) {
                     BlockHitResult placeResult = BlockUtils2.getPlaceResult(holePos, false, this.strictDirections.get());
                     if (placeResult != null) {
                        BlockUtils2.justPlace(result, placeResult, this.swing.get(), this.rotate.get(), 30);
                        this.renderBlocks.add(new RenderBlock(holePos, this.renderTime.get()));
                        this.delayLeft = this.placeDelay.get();
                        if (++blocksPlaced >= this.blocksPerTick.get()) {
                           break;
                        }
                     }
                  }
               });
            }
         }
      }
   }

   private boolean validHole(BlockPos pos, @Nullable Direction doubleHoleOffset, Block block) {
      if (this.mc.world.getBlockState(pos).getBlock().collidable) {
         return false;
      } else if (!this.mc.world.getBlockState(pos.up()).getBlock().collidable
            && !this.mc.world.getBlockState(pos.up(2)).getBlock().collidable
         || doubleHoleOffset != null
            && !this.mc.world.getBlockState(pos.offset(doubleHoleOffset).up()).getBlock().collidable
            && !this.mc.world.getBlockState(pos.offset(doubleHoleOffset).up(2)).getBlock().collidable) {
         Vec3d holePos = Vec3d.ofCenter(pos);
         if (BlockUtils2.outOfPlaceRange(pos, (Origin)this.placeOrigin.get(), this.placeRange.get())) {
            return false;
         } else if (BlockUtils2.outOfPlaceRange(pos, (Origin)this.placeOrigin.get(), this.wallsRange.get())
            && !UtilsPlus.canSeeBlock(pos, PlayerUtils2.eyePos(this.mc.player))) {
            return false;
         } else {
            Vec3d holePos2 = null;
            if (doubleHoleOffset != null) {
               BlockPos offsetBlock = pos.offset(doubleHoleOffset);
               holePos2 = Vec3d.ofCenter(offsetBlock);
               if (BlockUtils2.outOfPlaceRange(offsetBlock, (Origin)this.placeOrigin.get(), this.placeRange.get())) {
                  return false;
               }

               if (BlockUtils2.outOfPlaceRange(offsetBlock, (Origin)this.placeOrigin.get(), this.wallsRange.get())
                  && !UtilsPlus.canSeeBlock(offsetBlock, PlayerUtils2.eyePos(this.mc.player))) {
                  return false;
               }
            }

            if (!UtilsPlus.isSafe(this.mc.player)) {
               if (Math.sqrt(holePos.squaredDistanceTo(this.mc.player.getX(), holePos.y, this.mc.player.getZ()))
                  < this.safeRadiusH.get()) {
                  return false;
               }

               if (Math.sqrt(holePos.squaredDistanceTo(holePos.x, this.mc.player.getY(), holePos.z)) < this.safeRadiusV.get()) {
                  return false;
               }

               if (holePos2 != null) {
                  if (Math.sqrt(holePos2.squaredDistanceTo(this.mc.player.getX(), holePos2.y, this.mc.player.getZ()))
                     < this.safeRadiusH.get()) {
                     return false;
                  }

                  if (Math.sqrt(holePos2.squaredDistanceTo(holePos2.x, this.mc.player.getY(), holePos2.z)) < this.safeRadiusV.get()) {
                     return false;
                  }
               }
            }

            if (!this.mc.world.canPlace(block.getDefaultState(), pos, ShapeContext.absent())) {
               return false;
            } else if (!this.smart.get() || ((Keybind)this.forceFill.get()).isPressed()) {
               return true;
            } else if (this.onlyOnCa.get() && !Modules.get().isActive(AutoCrystal.class) && !Modules.get().isActive(CrystalAura.class)) {
               return false;
            } else {
               for(Entity entity : this.mc.world.getEntities()) {
                  if (entity.isAlive()
                     && entity != this.mc.player
                     && entity instanceof PlayerEntity player
                     && !((double)entity.distanceTo(this.mc.player) > Math.ceil(this.placeRange.get() + this.smartRadiusH.get()) + 1.0)
                     && Friends.get().shouldAttack(player)
                     && (!this.onlyMoving.get() || !UtilsPlus.isSurrounded(player, true, true) && UtilsPlus.smartVelocity(player).length() != 0.0)) {
                     Vec3d entityPos = entity.getPos();
                     if (this.predict.get()) {
                        entityPos = PlayerUtils2.predictPos(player, this.predictTicks.get(), 0);
                     }

                     if (entity.getBlockY() > pos.getY()
                        && !(Math.sqrt(holePos.squaredDistanceTo(holePos.x, entityPos.y - 0.5, holePos.z)) > this.smartRadiusV.get())
                        && (
                           !(Math.sqrt(holePos.squaredDistanceTo(entityPos.x, holePos.y, entityPos.z)) > this.smartRadiusH.get())
                              || holePos2 != null
                                 && !(
                                    Math.sqrt(holePos2.squaredDistanceTo(entityPos.x, holePos2.y, entityPos.z)) > this.smartRadiusH.get()
                                 )
                        )) {
                        return true;
                     }
                  }
               }

               return false;
            }
         }
      } else {
         return false;
      }
   }

   private Block getBlock(FindItemResult result) {
      return result.isOffhand()
         ? Block.getBlockFromItem(this.mc.player.getOffHandStack().getItem())
         : Block.getBlockFromItem(this.mc.player.getInventory().getStack(result.slot()).getItem());
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (!this.renderBlocks.isEmpty() && this.render.get()) {
         for(RenderBlock renderBlock : this.renderBlocks) {
            renderBlock.complexRender(
               event,
               (SettingColor)this.sideColor.get(),
               (SettingColor)this.lineColor.get(),
               (ShapeMode)this.shapeMode.get(),
               this.fade.get(),
               false,
               (RenderShape)this.renderShape.get(),
               this.width.get(),
               this.height.get(),
               this.yOffset.get(),
               this.weirdOffset.get()
            );
         }
      }
   }
}
