package venomhack.mixins.meteor;

import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.client.render.BlockBreakingInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(
   value = {BreakIndicators.class},
   remap = false
)
public class BreakIndicatorMixin {
   @Shadow(
      remap = false
   )
   @Final
   private Setting<SettingColor> startColor;
   @Shadow(
      remap = false
   )
   @Final
   private Setting<SettingColor> endColor;
   @Shadow(
      remap = false
   )
   @Final
   private Setting<ShapeMode> shapeMode;
   @Shadow(
      remap = false
   )
   @Final
   private final Color cSides = new Color();
   @Shadow(
      remap = false
   )
   @Final
   private final Color cLines = new Color();
   @Unique
   private final Map<Integer, BlockBreakingInfo> blocks = new HashMap<>();

   @Overwrite(
      remap = false
   )
   private void renderNormal(Render3DEvent event) {
      ClientPlayerInteractionManagerAccessor iam = (ClientPlayerInteractionManagerAccessor)MeteorClient.mc.interactionManager;
      BlockPos currentPos = iam.getCurrentBreakingBlockPos();
      boolean smooth = currentPos != null && iam.getBreakingProgress() >= 0.0F;
      if (smooth) {
         boolean notFound = true;

         for(BlockBreakingInfo info : this.blocks.values()) {
            if (info.getPos().equals(currentPos)) {
               notFound = false;
               break;
            }
         }

         if (notFound) {
            this.blocks.put(MeteorClient.mc.player.getId(), new BlockBreakingInfo(MeteorClient.mc.player.getId(), currentPos));
         }
      }

      for(BlockBreakingInfo info : this.blocks.values()) {
         BlockPos pos = info.getPos();
         int stage = info.getStage();
         VoxelShape shape = MeteorClient.mc.world.getBlockState(pos).getOutlineShape(MeteorClient.mc.world, pos);
         if (!shape.isEmpty()) {
            Box orig = shape.getBoundingBox();
            double shrinkFactor;
            if (smooth && iam.getCurrentBreakingBlockPos().equals(pos)) {
               shrinkFactor = 1.0 - (double)iam.getBreakingProgress();
            } else {
               shrinkFactor = (double)(9 - (stage + 1)) / 9.0;
            }

            double progress = 1.0 - shrinkFactor;
            Color c1Sides = ((SettingColor)this.startColor.get()).copy().a(((SettingColor)this.startColor.get()).a / 2);
            Color c2Sides = ((SettingColor)this.endColor.get()).copy().a(((SettingColor)this.endColor.get()).a / 2);
            SettingColor c1Lines = (SettingColor)this.startColor.get();
            SettingColor c2Lines = (SettingColor)this.endColor.get();
            this.cSides
               .set(
                  (int)Math.round((double)c1Sides.r + (double)(c2Sides.r - c1Sides.r) * progress),
                  (int)Math.round((double)c1Sides.g + (double)(c2Sides.g - c1Sides.g) * progress),
                  (int)Math.round((double)c1Sides.b + (double)(c2Sides.b - c1Sides.b) * progress),
                  (int)Math.round((double)c1Sides.a + (double)(c2Sides.a - c1Sides.a) * progress)
               );
            this.cLines
               .set(
                  (int)Math.round((double)c1Lines.r + (double)(c2Lines.r - c1Lines.r) * progress),
                  (int)Math.round((double)c1Lines.g + (double)(c2Lines.g - c1Lines.g) * progress),
                  (int)Math.round((double)c1Lines.b + (double)(c2Lines.b - c1Lines.b) * progress),
                  (int)Math.round((double)c1Lines.a + (double)(c2Lines.a - c1Lines.a) * progress)
               );
            Box var34 = orig.shrink(orig.getXLength() * shrinkFactor, orig.getYLength() * shrinkFactor, orig.getZLength() * shrinkFactor);
            progress = orig.getXLength() * shrinkFactor * 0.5;
            double yShrink = orig.getYLength() * shrinkFactor * 0.5;
            double zShrink = orig.getZLength() * shrinkFactor * 0.5;
            double x1 = (double)pos.getX() + var34.minX + progress;
            double y1 = (double)pos.getY() + var34.minY + yShrink;
            double z1 = (double)pos.getZ() + var34.minZ + zShrink;
            double x2 = (double)pos.getX() + var34.maxX + progress;
            double y2 = (double)pos.getY() + var34.maxY + yShrink;
            double z2 = (double)pos.getZ() + var34.maxZ + zShrink;
            event.renderer.box(x1, y1, z1, x2, y2, z2, this.cSides, this.cLines, (ShapeMode)this.shapeMode.get(), 0);
         }
      }
   }
}
