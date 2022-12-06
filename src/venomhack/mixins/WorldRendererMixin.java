package venomhack.mixins;

import com.mojang.blaze3d.platform.GlStateManager;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.modules.render.BetterChams;
import venomhack.modules.render.TanukiOutline;

@Mixin({WorldRenderer.class})
public abstract class WorldRendererMixin {
   @Shadow
   public static void drawShapeOutline(
      MatrixStack matrixStack, VertexConsumer vertexConsumer, VoxelShape voxelShape, double d, double e, double f, float g, float h, float i, float j
   ) {
   }

   @Inject(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw(Lnet/minecraft/client/render/RenderLayer;)V",
   ordinal = 13,
   shift = Shift.BEFORE
)}
   )
   private void renderItems(
      MatrixStack matrices,
      float tickDelta,
      long limitTime,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightmapTextureManager lightmapTextureManager,
      Matrix4f matrix4f,
      CallbackInfo info
   ) {
      BetterChams BetterChams = (BetterChams)Modules.get().get(BetterChams.class);
      if (BetterChams.isActive()) {
         GlStateManager._depthMask(true);
         GL11.glEnable(32823);
         GL11.glDepthRange(0.0, 0.01);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw(Lnet/minecraft/client/render/RenderLayer;)V",
   ordinal = 13,
   shift = Shift.AFTER
)}
   )
   private void renderItemsEnd(
      MatrixStack matrices,
      float tickDelta,
      long limitTime,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightmapTextureManager lightmapTextureManager,
      Matrix4f matrix4f,
      CallbackInfo info
   ) {
      BetterChams BetterChams = (BetterChams)Modules.get().get(BetterChams.class);
      if (BetterChams.isActive()) {
         GlStateManager._depthMask(false);
         GL11.glDisable(32823);
         GL11.glDepthRange(0.0, 1.0);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw(Lnet/minecraft/client/render/RenderLayer;)V",
   ordinal = 16,
   shift = Shift.AFTER
)}
   )
   private void renderGlint(
      MatrixStack matrices,
      float tickDelta,
      long limitTime,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightmapTextureManager lightmapTextureManager,
      Matrix4f matrix4f,
      CallbackInfo info
   ) {
      BetterChams BetterChams = (BetterChams)Modules.get().get(BetterChams.class);
      if (BetterChams.isActive()) {
         GlStateManager._depthMask(true);
         GL11.glEnable(32823);
         GL11.glDepthRange(0.0, 0.01);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw(Lnet/minecraft/client/render/RenderLayer;)V",
   ordinal = 22,
   shift = Shift.BEFORE
)}
   )
   private void renderGlintEnd(
      MatrixStack matrices,
      float tickDelta,
      long limitTime,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightmapTextureManager lightmapTextureManager,
      Matrix4f matrix4f,
      CallbackInfo info
   ) {
      BetterChams BetterChams = (BetterChams)Modules.get().get(BetterChams.class);
      if (BetterChams.isActive()) {
         GlStateManager._depthMask(false);
         GL11.glDisable(32823);
         GL11.glDepthRange(0.0, 1.0);
      }
   }

   @Inject(
      method = {"drawBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onDrawBlockOutline(
      MatrixStack matrixStack,
      VertexConsumer vertexConsumer,
      Entity entity,
      double d,
      double e,
      double f,
      BlockPos blockPos,
      BlockState blockState,
      CallbackInfo info
   ) {
      TanukiOutline tanukiOutline = (TanukiOutline)Modules.get().get(TanukiOutline.class);
      if (tanukiOutline.isActive()) {
         Vec3d colors = tanukiOutline.getColors();
         double alpha = tanukiOutline.getAlpha();
         drawShapeOutline(
            matrixStack,
            vertexConsumer,
            blockState.getOutlineShape(MeteorClient.mc.world, blockPos, ShapeContext.of(entity)),
            (double)blockPos.getX() - d,
            (double)blockPos.getY() - e,
            (double)blockPos.getZ() - f,
            (float)colors.x,
            (float)colors.y,
            (float)colors.z,
            (float)alpha
         );
         info.cancel();
      }
   }

   @Inject(
      method = {"setBlockBreakingInfo"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onBlockBreakingInfo(int entityId, BlockPos pos, int stage, CallbackInfo ci) {
      BreakIndicators bi = (BreakIndicators)Modules.get().get(BreakIndicators.class);
      if (((NoRender)Modules.get().get(NoRender.class)).noBlockBreakOverlay()) {
         ci.cancel();
      } else {
         if (bi.isActive() && 0 <= stage && stage <= 8) {
            ci.cancel();
         }
      }
   }
}
