package venomhack.modules.render;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class BurrowEsp extends ModuleHelper {
   private final Setting<Double> range = this.setting("range", "Maximum range of players.", Double.valueOf(11.0), 0.0, 256.0);
   private final Setting<Boolean> ignoreSelf = this.setting("ignore-self", "Ignore renders when for your own burrow", Boolean.valueOf(false));
   private final Setting<Boolean> ignoreFriend = this.setting("ignore-friend", "Ignore renders when for your friend's burrow", Boolean.valueOf(false));
   private final Setting<Boolean> renderText = this.setting("render-text", "Render text on the burrowed block", Boolean.valueOf(true));
   private final Setting<String> textOverlay = this.setting("text-overlay", "The text to render on burrowed block", "Burrowed");
   private final Setting<Double> textScale = this.setting("text-scale", "Scale of text to render on the block.", Double.valueOf(1.1), 0.0, 2.0);
   private final Setting<SettingColor> textColor = this.setting("text-color", "The text color.", 255, 255, 255, 255);
   private final Setting<Double> yOffset = this.setting("y-offset", "The y offset it would render from the bottom of the block", Double.valueOf(0.5), 0.0, 1.0);
   private final Setting<Boolean> renderBlock = this.setting("render-block", "Render the burrrowed block", Boolean.valueOf(false));
   private final Setting<Boolean> blockShape = this.setting("render-shape", "Render the block shape of the burrow block", Boolean.valueOf(true));
   private final Setting<ShapeMode> shapeMode = this.setting("shape-mode", "How the burrow block should be rendered", ShapeMode.Both);
   private final Setting<SettingColor> sideColor = this.setting("side-color", "The side color.", 255, 0, 0, 75);
   private final Setting<SettingColor> lineColor = this.setting("line-color", "The line color.", 255, 0, 0, 200);
   private final List<BlockPos> blockList = new ArrayList();

   public BurrowEsp() {
      super(Venomhack420.CATEGORY, "burrow-esp", "Shows you if player's are burrowed.");
   }

   @EventHandler
   private void onTick(Post event) {
      this.blockList.clear();

      for(AbstractClientPlayerEntity player : this.mc.world.getPlayers()) {
         if ((double)this.mc.player.distanceTo(player) > this.range.get()) {
            return;
         }

         if (this.ignoreSelf.get() && player == this.mc.player) {
            return;
         }

         if (this.ignoreFriend.get() && !Friends.get().shouldAttack(player)) {
            return;
         }

         BlockPos playerPos = new BlockPos(player.getPos().x, player.getPos().y + 0.4, player.getPos().z);
         if (this.mc.world.getBlockState(playerPos).getBlock().getBlastResistance() >= 600.0F) {
            this.blockList.add(playerPos);
         }
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (this.renderText.get()) {
         for(BlockPos blockPos : this.blockList) {
            Vec3 vec3 = new Vec3();
            vec3.set((double)blockPos.getX() + 0.5, (double)blockPos.getY() + this.yOffset.get(), (double)blockPos.getZ() + 0.5);
            if (NametagUtils.to2D(vec3, this.textScale.get())) {
               NametagUtils.begin(vec3);
               TextRenderer.get().begin(1.0, false, true);
               String text = (String)this.textOverlay.get();
               double w = TextRenderer.get().getWidth(text) / 2.0;
               TextRenderer.get().render(text, -w, 0.0, (Color)this.textColor.get(), true);
               TextRenderer.get().end();
               NametagUtils.end();
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.renderBlock.get()) {
         for(BlockPos blockPos : this.blockList) {
            if (this.blockShape.get()) {
               BlockState state = this.mc.world.getBlockState(blockPos);
               VoxelShape shape = state.getOutlineShape(this.mc.world, blockPos);
               if (this.shapeMode.get() == ShapeMode.Both || this.shapeMode.get() == ShapeMode.Lines) {
                  shape.forEachEdge(
                     (minX, minY, minZ, maxX, maxY, maxZ) -> event.renderer
                           .line(
                              (double)blockPos.getX() + minX,
                              (double)blockPos.getY() + minY,
                              (double)blockPos.getZ() + minZ,
                              (double)blockPos.getX() + maxX,
                              (double)blockPos.getY() + maxY,
                              (double)blockPos.getZ() + maxZ,
                              (Color)this.lineColor.get()
                           )
                  );
               }

               if (this.shapeMode.get() == ShapeMode.Both || this.shapeMode.get() == ShapeMode.Sides) {
                  for(Box box : shape.getBoundingBoxes()) {
                     event.renderer
                        .box(
                           (double)blockPos.getX() + box.minX,
                           (double)blockPos.getY() + box.minY,
                           (double)blockPos.getZ() + box.minZ,
                           (double)blockPos.getX() + box.maxX,
                           (double)blockPos.getY() + box.maxY,
                           (double)blockPos.getZ() + box.maxZ,
                           (Color)this.sideColor.get(),
                           (Color)this.lineColor.get(),
                           (ShapeMode)this.shapeMode.get(),
                           0
                        );
                  }
               }
            } else {
               event.renderer.box(blockPos, (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
            }
         }
      }
   }
}
