package venomhack.utils;

import java.util.Iterator;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.entity.EntityPose;

public class FakePlayerUtils {
   public static void onStatusEffectRemoved(FakePlayerEntity entity, StatusEffectInstance effect) {
      effect.getEffectType().onRemoved(entity, entity.getAttributes(), effect.getAmplifier());
   }

   public static void clearStatusEffects(FakePlayerEntity entity) {
      Iterator<StatusEffectInstance> iterator = entity.getActiveStatusEffects().values().iterator();

      while(iterator.hasNext()) {
         onStatusEffectRemoved(entity, (StatusEffectInstance)iterator.next());
         iterator.remove();
      }
   }

   public static void addStatusEffect(FakePlayerEntity entity, StatusEffectInstance effect) {
      StatusEffectInstance statusEffectInstance = (StatusEffectInstance)entity.getActiveStatusEffects().get(effect.getEffectType());
      if (statusEffectInstance == null) {
         entity.getActiveStatusEffects().put(effect.getEffectType(), effect);
         effect.getEffectType().onApplied(entity, entity.getAttributes(), effect.getAmplifier());
      } else {
         if (statusEffectInstance.upgrade(effect)) {
            StatusEffect statusEffect = effect.getEffectType();
            statusEffect.onRemoved(entity, entity.getAttributes(), effect.getAmplifier());
            statusEffect.onApplied(entity, entity.getAttributes(), effect.getAmplifier());
         }
      }
   }

   public static void decreaseHealth(FakePlayerEntity entity, float damage) {
      float health = UtilsPlus.getTotalHealth(entity);
      float newHealth = health - damage;
      if (newHealth <= 0.0F) {
         entity.setHealth(1.0F);
         clearStatusEffects(entity);
         addStatusEffect(entity, new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
         addStatusEffect(entity, new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
         addStatusEffect(entity, new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));

         try {
            ClientConnection.handlePacket(new EntityStatusS2CPacket(entity, (byte)35), MeteorClient.mc.player.networkHandler);
         } catch (Exception var5) {
         }
      } else {
         if (newHealth <= entity.getMaxHealth()) {
            entity.setAbsorptionAmount(0.0F);
            entity.setHealth(newHealth);
         } else {
            entity.setAbsorptionAmount(newHealth - entity.getMaxHealth());
         }
      }
   }

   public static void updatePose(FakePlayerEntity entity) {
      if (entity.wouldPoseNotCollide(EntityPose.SWIMMING)) {
         EntityPose entityPose;
         if (entity.isFallFlying()) {
            entityPose = EntityPose.FALL_FLYING;
         } else if (entity.isSleeping()) {
            entityPose = EntityPose.SLEEPING;
         } else if (entity.isSwimming()) {
            entityPose = EntityPose.SWIMMING;
         } else if (entity.isUsingRiptide()) {
            entityPose = EntityPose.SPIN_ATTACK;
         } else if (entity.isSneaking() && !entity.getAbilities().flying) {
            entityPose = EntityPose.CROUCHING;
         } else {
            entityPose = EntityPose.STANDING;
         }

         EntityPose entityPose2;
         if (entity.isSpectator() || entity.hasVehicle() || entity.wouldPoseNotCollide(entityPose)) {
            entityPose2 = entityPose;
         } else if (entity.wouldPoseNotCollide(EntityPose.CROUCHING)) {
            entityPose2 = EntityPose.CROUCHING;
         } else {
            entityPose2 = EntityPose.SWIMMING;
         }

         entity.setPose(entityPose2);
      }
   }
}
