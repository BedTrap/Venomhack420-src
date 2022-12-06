package venomhack.modules.movement;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.FallingBlock;
import net.minecraft.util.math.BlockPos.class_2339;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;
import venomhack.utils.customObjects.RenderBlock;

public class Scaffold extends ModuleHelper {
   private final SettingGroup sgRender = this.group("Render");
   private final Setting<List<Block>> blocks = this.setting("blocks", "Selected blocks.", this.sgGeneral, null, new Block[]{Blocks.ANVIL});
   private final Setting<Scaffold.ListMode> blocksFilter = this.setting("blocks-filter", "How to use the block list setting.", Scaffold.ListMode.Blacklist);
   public final Setting<Boolean> fastTower = this.setting("fast-tower", "Whether or not to scaffold upwards faster.", Boolean.valueOf(false));
   public final Setting<Double> towerSpeed = this.setting("up-speed", "How fast to go up.", 0.4, this.sgGeneral, this.fastTower::get, 0.0, 1.0, 2);
   public final Setting<Double> towerPullSpeed = this.setting("pull-speed", "How fast to go down.", 0.28, this.sgGeneral, this.fastTower::get, 0.0, 1.0, 2);
   public final Setting<Double> towerTimer = this.setting(
      "tower-timer", "What timer speed to use for towering up.", Double.valueOf(1.0), this.sgGeneral, this.fastTower::get, 0.0, 5.0
   );
   private final Setting<Boolean> autoSwitch = this.setting("auto-switch", "Automatically swaps to a block before placing.", Boolean.valueOf(true));
   private final Setting<Boolean> rotate = this.setting("rotate", "Rotates towards the blocks being placed.", Boolean.valueOf(true));
   private final Setting<Boolean> renderSwing = this.setting("swing", "Renders the swing client side.", Boolean.valueOf(false), this.sgRender);
   private final Setting<Boolean> render = this.setting("render", "Renders the block where it is placing a crystal.", Boolean.valueOf(true), this.sgRender);
   private final Setting<ShapeMode> shapeMode = this.setting("shape-mode", "How the shapes are rendered.", ShapeMode.Both, this.sgRender, this.render::get);
   private final Setting<Integer> renderTime = this.setting(
      "render-time", "Ticks to render the block for.", Integer.valueOf(8), this.sgRender, this.render::get
   );
   private final Setting<Boolean> fade = this.setting(
      "fade", "Will reduce the opacity of the rendered block over time.", Boolean.valueOf(true), this.sgRender, this.render::get
   );
   private final Setting<SettingColor> sideColor = this.setting(
      "side-color", "The side color of the target block rendering.", 197, 137, 232, 10, this.sgRender, this.render::get
   );
   private final Setting<SettingColor> lineColor = this.setting(
      "line-color", "The line color of the target block rendering.", 197, 137, 232, this.sgRender, this.render::get
   );
   private final List<RenderBlock> renderBlocks = new ArrayList<>();
   private final class_2339 blockPos = new class_2339();
   private boolean lastWasSneaking;
   private double lastSneakingY;

   public Scaffold() {
      super(Venomhack420.CATEGORY, "scaffold-vh", "Automatically places blocks under you.");
   }

   public void onDeactivate() {
      this.renderBlocks.clear();
      ((Timer)Modules.get().get(Timer.class)).setOverride(1.0);
   }

   public void onActivate() {
      this.lastWasSneaking = this.mc.options.sneakKey.isPressed();
      if (this.lastWasSneaking) {
         this.lastSneakingY = this.mc.player.getY();
      }

      this.renderBlocks.clear();
   }

   @EventHandler
   private void onTick(Post event) {
      RenderBlock.tick(this.renderBlocks);
      this.blockPos.set(this.mc.player.getBlockPos().down());
      FindItemResult item = InvUtils.findInHotbar(itemStack -> this.validItem(itemStack, this.blockPos));
      if (item.found()) {
         if (item.getHand() != null || this.autoSwitch.get()) {
            if (this.mc.options.sneakKey.isPressed() && !this.mc.options.jumpKey.isPressed()) {
               if (this.lastSneakingY - this.mc.player.getY() < 0.1) {
                  this.lastWasSneaking = false;
                  return;
               }
            } else {
               this.lastWasSneaking = false;
            }

            if (!this.lastWasSneaking) {
               this.lastSneakingY = this.mc.player.getY();
            }

            if (this.mc.options.jumpKey.isPressed() && !this.mc.options.sneakKey.isPressed() && this.fastTower.get()) {
               ((Timer)Modules.get().get(Timer.class)).setOverride(this.towerTimer.get());
               if (this.mc.player.isOnGround()) {
                  this.mc
                     .player
                     .setVelocity(
                        this.mc.player.getVelocity().x * 0.3 / this.towerTimer.get(),
                        this.towerSpeed.get(),
                        this.mc.player.getVelocity().z * 0.3 / this.towerTimer.get()
                     );
               }

               if (this.mc.world.getBlockState(this.mc.player.getBlockPos().down()).isAir()) {
                  if (BlockUtils.place(this.blockPos, item, this.rotate.get(), 50, this.renderSwing.get(), false)) {
                     this.renderBlocks.add(new RenderBlock(this.blockPos, this.renderTime.get()));
                  }

                  this.mc
                     .player
                     .setVelocity(
                        this.mc.player.getVelocity().x, -1.0 * this.towerPullSpeed.get(), this.mc.player.getVelocity().z
                     );
               }
            } else {
               ((Timer)Modules.get().get(Timer.class)).setOverride(1.0);
               if (BlockUtils.place(this.blockPos, item, this.rotate.get(), 50, this.renderSwing.get(), false)) {
                  this.renderBlocks.add(new RenderBlock(this.blockPos, 8));
               }
            }
         }
      }
   }

   private boolean validItem(ItemStack itemStack, BlockPos pos) {
      if (!(itemStack.getItem() instanceof BlockItem)) {
         return false;
      } else {
         Block block = ((BlockItem)itemStack.getItem()).getBlock();
         if (this.blocksFilter.get() == Scaffold.ListMode.Blacklist && ((List)this.blocks.get()).contains(block)) {
            return false;
         } else if (this.blocksFilter.get() == Scaffold.ListMode.Whitelist && !((List)this.blocks.get()).contains(block)) {
            return false;
         } else if (!Block.isShapeFullCube(block.getDefaultState().getCollisionShape(this.mc.world, pos))) {
            return false;
         } else {
            return !(block instanceof FallingBlock) || !FallingBlock.canFallThrough(this.mc.world.getBlockState(pos));
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (!this.renderBlocks.isEmpty() && this.render.get()) {
         this.renderBlocks
            .forEach(
               renderBlock -> renderBlock.render(
                     event, (SettingColor)this.sideColor.get(), (SettingColor)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), this.fade.get()
                  )
            );
      }
   }

   public static enum ListMode {
      Whitelist,
      Blacklist;
   }
}
