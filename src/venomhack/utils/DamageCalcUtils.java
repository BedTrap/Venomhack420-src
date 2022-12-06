package venomhack.utils;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.GameMode;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.world.explosion.Explosion.class_4179;
import net.minecraft.util.hit.HitResult.class_240;
import net.minecraft.world.RaycastContext.class_242;
import net.minecraft.world.RaycastContext.class_3960;
import venomhack.mixinInterface.IVec3d;
import venomhack.modules.misc.PacketMine;

public class DamageCalcUtils {
   public static Explosion explosion;
   private static PacketMine packetMine;
   private static final Vec3d vec = new Vec3d(0.0, 0.0, 0.0);
   private static final IVec3d iVec = (IVec3d)vec;

   public static float explosionDamage(LivingEntity target, Vec3d pos, int explosionPower) {
      return explosionDamage(target, pos, false, 0, 0, false, false, true, explosionPower);
   }

   public static float explosionDamage(LivingEntity target, Vec3d pos, boolean ignoreTerrain, boolean predict, boolean withExposure, int explosionPower) {
      return explosionDamage(target, pos, predict, 1, 1, ignoreTerrain, false, withExposure, explosionPower);
   }

   public static float explosionDamage(
      LivingEntity target,
      Vec3d pos,
      boolean predict,
      int predictTicks,
      int antiStepOffset,
      boolean ignoreTerrain,
      boolean placing,
      boolean withExposure,
      int explosionPower
   ) {
      Vec3d targetPos = target.getPos();
      if (target instanceof PlayerEntity player) {
         if (EntityUtils.getGameMode(player) == GameMode.CREATIVE) {
            return 0.0F;
         }

         if (predict) {
            targetPos = PlayerUtils2.predictPos(player, predictTicks, antiStepOffset);
         }
      }

      int explosionRadius = explosionPower * 2;
      if (targetPos.distanceTo(pos) > (double)explosionRadius) {
         return 0.0F;
      } else {
         double impact = (1.0 - Math.sqrt(target.squaredDistanceTo(pos)) / (double)explosionRadius)
            * (double)(withExposure ? getExposure(pos, target, predict, predictTicks, antiStepOffset, ignoreTerrain, placing) : 1.0F);
         float damage = (float)((impact * impact + impact) * (double)explosionRadius * 3.5 + 1.0);
         switch(MeteorClient.mc.world.getDifficulty()) {
            case HARD:
               damage = (float)((double)damage * 1.5);
               break;
            case PEACEFUL:
               return 0.0F;
            case EASY:
               damage = damage < 2.0F ? damage : damage * 0.5F + 1.0F;
         }

         damage = DamageUtil.getDamageLeft(damage, (float)target.getArmor(), (float)target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
         StatusEffectInstance resistance = target.getStatusEffect(StatusEffects.RESISTANCE);
         if (resistance != null) {
            damage = Math.max((float)(25 - (resistance.getAmplifier() + 1) * 5) * damage * 0.04F, 0.0F);
         }

         int i = EnchantmentHelper.getProtectionAmount(target.getArmorItems(), DamageSource.explosion(explosion));
         if (i > 0) {
            damage = DamageUtil.getInflictedDamage(damage, (float)i);
         }

         return damage < 0.0F ? 0.0F : damage;
      }
   }

   public static float getExposure(Vec3d source, Entity entity, boolean predict, boolean ignoreTerrain) {
      return getExposure(source, entity, predict, ignoreTerrain, false);
   }

   private static float getExposure(Vec3d source, Entity entity, boolean predict, boolean ignoreTerrain, boolean placing) {
      return getExposure(source, entity, predict, 1, 1, ignoreTerrain, placing);
   }

   private static float getExposure(
      Vec3d source, Entity entity, boolean predict, int predictTicks, int antiStepOffset, boolean ignoreTerrain, boolean placing
   ) {
      Box box = entity.getBoundingBox();
      if (entity instanceof PlayerEntity player && predict) {
         box = PlayerUtils2.predictBox(player, predictTicks, antiStepOffset);
      }

      double xWidthReciprocal = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
      double yHeightReciprocal = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
      double zWidthReciprocal = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
      if (!(xWidthReciprocal < 0.0) && !(yHeightReciprocal < 0.0) && !(zWidthReciprocal < 0.0)) {
         double g = (1.0 - Math.floor(1.0 / xWidthReciprocal) * xWidthReciprocal) * 0.5;
         double h = (1.0 - Math.floor(1.0 / zWidthReciprocal) * zWidthReciprocal) * 0.5;
         float nonSolid = 0.0F;
         int total = 0;
         RaycastContext raycastContext = new RaycastContext(vec, source, class_3960.COLLIDER, class_242.NONE, entity);

         for(float x = 0.0F; x <= 1.0F; x = (float)((double)x + xWidthReciprocal)) {
            double lerpX = MathHelper.lerp((double)x, box.minX, box.maxX);
            iVec.setX(lerpX + g);

            for(float y = 0.0F; y <= 1.0F; y = (float)((double)y + yHeightReciprocal)) {
               double lerpY = MathHelper.lerp((double)y, box.minY, box.maxY);
               iVec.setY(lerpY);

               for(float z = 0.0F; z <= 1.0F; z = (float)((double)z + zWidthReciprocal)) {
                  double lerpZ = MathHelper.lerp((double)z, box.minZ, box.maxZ);
                  iVec.setZ(lerpZ + h);
                  if (raycast(raycastContext, ignoreTerrain, placing).getType() == class_240.MISS) {
                     ++nonSolid;
                  }

                  ++total;
               }
            }
         }

         return nonSolid / (float)total;
      } else {
         return 0.0F;
      }
   }

   private static BlockHitResult raycast(RaycastContext context, boolean ignoreTerrain, boolean placing) {
      return (BlockHitResult)BlockView.raycast(
         context.getStart(),
         context.getEnd(),
         context,
         (raycastContext, pos) -> {
            BlockState blockState = MeteorClient.mc.world.getBlockState(pos);
            if (packetMine.isActive()) {
               blockState = packetMine.getState(pos);
            }
   
            BlockPos endBlockPos = new BlockPos(raycastContext.getEnd());
            if (pos.equals(endBlockPos.down())) {
               if (placing) {
                  blockState = Blocks.OBSIDIAN.getDefaultState();
               }
            } else if (pos.equals(endBlockPos)) {
               if (blockState.isOf(Blocks.RESPAWN_ANCHOR) || blockState.getBlock() instanceof BedBlock) {
                  blockState = Blocks.AIR.getDefaultState();
               }
            } else if (blockState.getBlock() instanceof BedBlock) {
               BlockState endState = MeteorClient.mc.world.getBlockState(endBlockPos);
               if (packetMine.isActive()) {
                  endState = packetMine.getState(endBlockPos);
               }
   
               if (endState.getBlock() instanceof BedBlock && pos.offset(BedBlock.getOppositePartDirection(blockState)).equals(endBlockPos)) {
                  blockState = Blocks.AIR.getDefaultState();
               }
            }
   
            if (ignoreTerrain && blockState.getBlock().getBlastResistance() < 600.0F) {
               blockState = Blocks.AIR.getDefaultState();
            }
   
            BlockHitResult blockHitResult = MeteorClient.mc
               .world
               .raycastBlock(
                  raycastContext.getStart(),
                  raycastContext.getEnd(),
                  pos,
                  raycastContext.getBlockShape(blockState, MeteorClient.mc.world, pos),
                  blockState
               );
            BlockHitResult fluidHitResult = raycastContext.getFluidShape(MeteorClient.mc.world.getFluidState(pos), MeteorClient.mc.world, pos)
               .raycast(raycastContext.getStart(), raycastContext.getEnd(), pos);
            double blockDistance = blockHitResult == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult.getPos());
            double fluidDistance = fluidHitResult == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(fluidHitResult.getPos());
            return blockDistance <= fluidDistance ? blockHitResult : fluidHitResult;
         },
         raycastContext -> {
            Direction direction = Direction.getFacing(
               raycastContext.getStart().x - raycastContext.getEnd().x,
               raycastContext.getStart().y - raycastContext.getEnd().y,
               raycastContext.getStart().z - raycastContext.getEnd().z
            );
            return BlockHitResult.createMissed(raycastContext.getEnd(), direction, new BlockPos(raycastContext.getEnd()));
         }
      );
   }

   public static void init() {
      MeteorClient.EVENT_BUS.subscribe(DamageCalcUtils.class);
   }

   @EventHandler
   private static void onGameJoined(GameJoinedEvent event) {
      explosion = new Explosion(MeteorClient.mc.world, null, 0.0, 0.0, 0.0, 6.0F, false, class_4179.DESTROY);
      if (packetMine == null) {
         packetMine = (PacketMine)Modules.get().get(PacketMine.class);
      }
   }
}
