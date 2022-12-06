package venomhack.modules.render;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.sound.SoundEvents;
import venomhack.Venomhack420;
import venomhack.events.PlayerDeathEvent;
import venomhack.modules.ModuleHelper;

public class KillEffects extends ModuleHelper {
   private final SettingGroup sgLightning = this.group("Lightning");
   private final SettingGroup sgFirework = this.group("Firework");
   private final Setting<KillEffects.FxMode> mode = this.setting("mode", "The kill effect mode.", KillEffects.FxMode.LIGHTNING, this.sgGeneral);
   private final Setting<Integer> multiplier = this.setting(
      "Multiplier", "The amount of effects to render.", Integer.valueOf(1), this.sgGeneral, 1.0, 10.0, 1, 32767
   );
   private final Setting<Boolean> coolSound = this.setting(
      "trident-sound", "Makes the thunder sound cooler", Boolean.valueOf(false), this.sgLightning, () -> this.mode.get() == KillEffects.FxMode.LIGHTNING
   );
   private final Setting<Boolean> clip = this.setting(
      "clip",
      "Whether the fireworks should clip through blocks or not.",
      Boolean.valueOf(false),
      this.sgFirework,
      () -> this.mode.get() == KillEffects.FxMode.FIREWORK
   );

   public KillEffects() {
      super(Venomhack420.CATEGORY, "kill-fx", "Various effects when killing a player.");
   }

   @EventHandler
   public void onDeath(PlayerDeathEvent event) {
      if (event.isTarget()) {
         Vec3d pos = event.getPlayer().getPos();

         for(int i = 0; i < this.multiplier.get(); ++i) {
            if (this.mode.get() == KillEffects.FxMode.LIGHTNING) {
               LightningEntity lightningBolt = new LightningEntity(EntityType.LIGHTNING_BOLT, this.mc.world);
               lightningBolt.setPosition(pos);
               this.mc.world.addEntity(93, lightningBolt);
               if (this.coolSound.get()) {
                  this.mc.player.playSound(SoundEvents.ITEM_TRIDENT_THUNDER, 2.0F, 1.0F);
               }
            }

            if (this.mode.get() == KillEffects.FxMode.FIREWORK) {
               FireworkRocketEntity rocket = new FireworkRocketEntity(EntityType.FIREWORK_ROCKET, this.mc.world);
               rocket.setPosition(pos);
               this.mc.world.addEntity(72, rocket);
               if (this.clip.get()) {
                  rocket.noClip = true;
               }

               this.mc.player.playSound(SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0F, 1.0F);
            }
         }
      }
   }

   public static enum FxMode {
      LIGHTNING("Lightning"),
      FIREWORK("Firework");

      private final String title;

      private FxMode(String title) {
         this.title = title;
      }

      @Override
      public String toString() {
         return this.title;
      }
   }
}
