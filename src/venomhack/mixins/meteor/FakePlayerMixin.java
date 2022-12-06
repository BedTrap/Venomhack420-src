package venomhack.mixins.meteor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.player.FakePlayer;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import venomhack.utils.DamageCalcUtils;
import venomhack.utils.FakePlayerUtils;
import venomhack.utils.customObjects.AnglePos;

@Mixin(
   value = {FakePlayer.class},
   remap = false
)
public class FakePlayerMixin {
   @Shadow
   @Final
   private SettingGroup sgGeneral;
   @Shadow
   @Final
   public Setting<String> name;
   @Shadow
   @Final
   public Setting<Boolean> copyInv;
   @Shadow
   @Final
   public Setting<Integer> health;
   @Unique
   private Setting<Boolean> loop = null;
   @Unique
   private Setting<Boolean> damageCalcs = null;
   @Unique
   private boolean recording = false;
   @Unique
   private final List<AnglePos> posList = new ArrayList<>();
   @Unique
   private boolean playing = false;
   @Unique
   private static final Random random = new Random();
   @Unique
   private int gapDelay = 0;
   @Unique
   private int index = 0;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")},
      remap = false
   )
   private void onInit(CallbackInfo ci) {
      this.loop = this.sgGeneral
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("loop")).description("Whether to loop the recorded movement after playing.")).defaultValue(true))
               .build()
         );
      this.damageCalcs = this.sgGeneral
         .add(
            ((Builder)((Builder)((Builder)new Builder().name("damage-calcs")).description("Whether to apply explosion damage to fake players or not."))
                  .defaultValue(true))
               .build()
         );
   }

   @Inject(
      method = {"onActivate"},
      at = {@At("HEAD")},
      remap = false
   )
   private void onActivate(CallbackInfo ci) {
      this.recording = false;
      this.playing = false;
      this.gapDelay = 0;
      this.index = 0;
      this.posList.clear();
   }

   @Overwrite(
      remap = false
   )
   public WWidget getWidget(GuiTheme theme) {
      WHorizontalList hList = theme.horizontalList();
      WButton spawn = (WButton)hList.add(theme.button("Spawn")).widget();
      spawn.action = () -> {
         if (((Module)this).isActive()) {
            FakePlayerManager.add((String)this.name.get(), (float)((Integer)this.health.get()).intValue(), this.copyInv.get());
         }
      };
      WButton clear = (WButton)hList.add(theme.button("Clear")).widget();
      clear.action = () -> {
         if (((Module)this).isActive()) {
            FakePlayerManager.clear();
         }
      };
      WButton start = (WButton)hList.add(theme.button("Start Recording")).widget();
      start.action = () -> {
         this.posList.clear();
         this.recording = true;
      };
      WButton stop = (WButton)hList.add(theme.button("Stop Recording")).widget();
      stop.action = () -> this.recording = false;
      WButton play = (WButton)hList.add(theme.button("Play")).widget();
      play.action = () -> {
         this.playing = true;
         this.index = 0;
      };
      WButton stopPlay = (WButton)hList.add(theme.button("Pause")).widget();
      stopPlay.action = () -> this.playing = false;
      return hList;
   }

   @Unique
   @EventHandler
   private void onPacket(Receive event) {
      if (this.damageCalcs.get()) {
         Packet var3 = event.packet;
         if (var3 instanceof ExplosionS2CPacket packet) {
            FakePlayerManager.forEach(
               entity -> {
                  float damage = DamageCalcUtils.explosionDamage(
                     entity, new Vec3d(packet.getX(), packet.getY(), packet.getZ()), (int)packet.getRadius()
                  );
                  if (!(damage <= 0.0F)) {
                     if (entity.timeUntilRegen <= 10) {
                        entity.hurtTime = 10;
                        entity.timeUntilRegen = 20;
                        entity.limbDistance = 1.5F;
                        MeteorClient.mc
                           .world
                           .playSound(
                              entity.getBlockPos(),
                              SoundEvents.ENTITY_PLAYER_HURT,
                              SoundCategory.PLAYERS,
                              1.0F,
                              (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                              true
                           );
                        FakePlayerUtils.decreaseHealth(entity, damage);
                     }
                  }
               }
            );
         }
      }
   }

   @Unique
   @EventHandler
   private void onTick(Post event) {
      FakePlayerManager.forEach(entity -> {
         FakePlayerUtils.updatePose(entity);
         if (this.gapDelay == 0) {
            FakePlayerUtils.addStatusEffect(entity, new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1));
            FakePlayerUtils.addStatusEffect(entity, new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));
            FakePlayerUtils.addStatusEffect(entity, new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0));
            FakePlayerUtils.addStatusEffect(entity, new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3));
            this.gapDelay = 32;
         } else {
            --this.gapDelay;
         }
      });
      if (this.recording) {
         this.posList
            .add(
               new AnglePos(
                  MeteorClient.mc.player.getPos(),
                  MeteorClient.mc.player.getYaw(),
                  MeteorClient.mc.player.getPitch(),
                  MeteorClient.mc.player.getHeadYaw(),
                  MeteorClient.mc.player.getPose()
               )
            );
      }

      if (this.playing) {
         if (this.posList.isEmpty()) {
            return;
         }

         if (this.index >= this.posList.size()) {
            if (!this.loop.get()) {
               this.playing = false;
               return;
            }

            this.index = 0;
         }

         AnglePos pose = this.posList.get(this.index++);
         FakePlayerManager.forEach(entity -> {
            entity.updateTrackedPositionAndAngles(pose.vec().x, pose.vec().y, pose.vec().z, pose.yaw(), pose.pitch(), 3, false);
            entity.updateTrackedHeadRotation(pose.headYaw(), 3);
         });
      }
   }
}
