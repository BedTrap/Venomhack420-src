package venomhack.utils.customObjects;

import java.util.List;
import javax.annotation.Nullable;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import venomhack.enums.RenderShape;
import venomhack.mixinInterface.IRenderer3D;
import venomhack.utils.RenderUtils2;

public class RenderBlock {
   public BlockPos pos;
   public float ticks;
   public float damage;
   public float selfDamage;
   private BlockState state;
   protected int maxTicks;

   public static synchronized void addRenderBlock(List<RenderBlock> renderBlocks, BlockPos pos, int renderTime) {
      addRenderBlock(renderBlocks, pos, renderTime, 100, 0.0F, 0.0F, null);
   }

   public static synchronized void addRenderBlock(List<RenderBlock> renderBlocks, BlockPos pos, int renderTime, BlockState state) {
      addRenderBlock(renderBlocks, pos, renderTime, 100, 0.0F, 0.0F, state);
   }

   public static synchronized void addRenderBlock(List<RenderBlock> renderBlocks, BlockPos pos, int renderTime, int maxBlocks, float damage) {
      addRenderBlock(renderBlocks, pos, renderTime, maxBlocks, damage, 0.0F, null);
   }

   public static synchronized void addRenderBlock(
      List<RenderBlock> renderBlocks, BlockPos pos, int renderTime, int maxBlocks, float damage, float selfDamage
   ) {
      addRenderBlock(renderBlocks, pos, renderTime, maxBlocks, damage, selfDamage, null);
   }

   public static synchronized void addRenderBlock(
      List<RenderBlock> renderBlocks, BlockPos pos, int renderTime, int maxBlocks, float damage, float selfDamage, BlockState state
   ) {
      boolean found = false;

      for(RenderBlock block : renderBlocks) {
         if (block.pos.equals(pos)) {
            block.set(renderTime, damage, selfDamage, state);
            found = true;
         }
      }

      if (!found) {
         if (!renderBlocks.isEmpty()) {
            while(renderBlocks.size() >= maxBlocks) {
               renderBlocks.remove(0);
            }
         }

         renderBlocks.add(new RenderBlock(pos, renderTime, damage, selfDamage, state));
      }
   }

   public static synchronized void tick(List<RenderBlock> list) {
      for(RenderBlock block : list) {
         --block.ticks;
      }

      list.removeIf(blockx -> blockx.ticks <= 0.0F);
   }

   public RenderBlock(BlockPos pos, int ticks) {
      this.pos = pos;
      this.ticks = (float)ticks;
      this.maxTicks = ticks;
   }

   public RenderBlock(BlockPos pos, int ticks, float damage) {
      this.pos = pos;
      this.ticks = (float)ticks;
      this.damage = damage;
      this.maxTicks = ticks;
   }

   public RenderBlock(BlockPos pos, int ticks, float damage, float selfDamage, BlockState state) {
      this.pos = pos;
      this.ticks = (float)ticks;
      this.damage = damage;
      this.selfDamage = selfDamage;
      this.state = state;
      this.maxTicks = ticks;
   }

   public void render(Render3DEvent event, SettingColor sideColor, SettingColor lineColor, ShapeMode shapeMode, boolean fade) {
      this.complexRender(event, sideColor, lineColor, shapeMode, fade, 0, false, RenderShape.CUBOID, 1.0, 1.0, 0.0, 0.0, false);
   }

   public void complexRender(
      Render3DEvent event,
      SettingColor sideColor,
      SettingColor lineColor,
      ShapeMode shapeMode,
      boolean fade,
      boolean advanced,
      RenderShape renderShape,
      double width,
      double height,
      double yOffset,
      double weirdOffset
   ) {
      this.complexRender(event, sideColor, lineColor, shapeMode, fade, 0, advanced, renderShape, width, height, yOffset, weirdOffset, false);
   }

   public void complexRender(
      Render3DEvent event,
      SettingColor sideColor,
      SettingColor lineColor,
      ShapeMode shapeMode,
      boolean fade,
      int beforeFadeDelay,
      boolean advanced,
      RenderShape renderShape,
      double width,
      double height,
      double yOffset,
      double weirdOffset
   ) {
      this.complexRender(event, sideColor, lineColor, shapeMode, fade, beforeFadeDelay, advanced, renderShape, width, height, yOffset, weirdOffset, false);
   }

   public void complexRender(
      Render3DEvent event,
      SettingColor sideColor,
      SettingColor lineColor,
      ShapeMode shapeMode,
      boolean fade,
      int beforeFadeDelay,
      boolean advanced,
      RenderShape renderShape,
      double width,
      double height,
      double yOffset,
      double weirdOffset,
      boolean shrink
   ) {
      complexRender(
         event,
         (double)this.pos.getX(),
         (double)this.pos.getY(),
         (double)this.pos.getZ(),
         this.state,
         sideColor,
         lineColor,
         shapeMode,
         fade,
         this.ticks,
         this.maxTicks,
         beforeFadeDelay,
         advanced,
         renderShape,
         width,
         height,
         yOffset,
         weirdOffset,
         shrink
      );
   }

   public static void complexRender(
      Render3DEvent event,
      double xPos,
      double yPos,
      double zPos,
      @Nullable BlockState state,
      SettingColor sideColor,
      SettingColor lineColor,
      ShapeMode shapeMode,
      boolean fade,
      float ticks,
      int maxTicks,
      int beforeFadeDelay,
      boolean advanced,
      RenderShape renderShape,
      double width,
      double height,
      double yOffset,
      double weirdOffset,
      boolean shrink
   ) {
      int preSideA = sideColor.a;
      int preLineA = lineColor.a;
      float factor = ticks > (float)(maxTicks - beforeFadeDelay) ? 1.0F : ticks / (float)maxTicks;
      if (fade) {
         sideColor.a = (int)((float)sideColor.a * factor);
         lineColor.a = (int)((float)lineColor.a * factor);
      }

      if (shrink) {
         width *= (double)factor;
         height *= (double)factor;
      }

      double x = xPos + (1.0 - width) * 0.5;
      double y = yPos + (1.0 - height) * 0.5 + yOffset;
      double z = zPos + (1.0 - width) * 0.5;
      IRenderer3D renderer3D = (IRenderer3D)event.renderer;
      switch(renderShape) {
         case CUBOID:
            if (advanced && state != null) {
               RenderUtils2.renderState(event.renderer, state, new BlockPos(xPos, yPos, zPos), shapeMode, sideColor, lineColor);
            } else {
               event.renderer.box(x, y, z, x + width, y + height, z + width, sideColor, lineColor, shapeMode, 0);
            }
            break;
         case CORNER_PYRAMIDS:
            renderer3D.fancyThing(x, y, z, x + width, y + height, z + width, sideColor, lineColor, shapeMode, weirdOffset);
            break;
         case OCTAHEDRON:
            renderer3D.octahedron(x, y, z, x + width, y + height, z + width, sideColor, lineColor, shapeMode);
      }

      sideColor.a = preSideA;
      lineColor.a = preLineA;
   }

   public synchronized void set(int ticks) {
      this.ticks = (float)ticks;
   }

   public synchronized void set(int ticks, BlockState state) {
      this.ticks = (float)ticks;
      this.state = state;
   }

   public synchronized void set(int ticks, float damage) {
      this.ticks = (float)ticks;
      this.damage = damage;
   }

   public synchronized void set(int ticks, float damage, float selfDamage, BlockState state) {
      this.ticks = (float)ticks;
      this.damage = damage;
      this.selfDamage = selfDamage;
      this.state = state;
   }
}
