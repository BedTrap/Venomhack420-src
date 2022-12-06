package venomhack.modules.render;

import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import venomhack.Venomhack420;
import venomhack.modules.ModuleHelper;

public class BetterPops extends ModuleHelper {
   private final SettingGroup sgSelf = this.group("Self");
   private final SettingGroup sgOther = this.group("Other");
   public final Setting<Boolean> friendsCopySelf = this.setting("friends-copy-self", "Friends copy self settings.", Boolean.valueOf(false), this.sgGeneral);
   public final Setting<Boolean> changeColorSelf = this.setting("change-color-self", "Whether or not to change color.", Boolean.valueOf(true));
   public final Setting<SettingColor> colorOneSelf = this.setting(
      "color-one-self", "The first confetti color to change.", 255, 255, 255, 255, this.sgSelf, this.changeColorSelf::get
   );
   public final Setting<SettingColor> colorTwoSelf = this.setting(
      "color-two-self", "The second confetti color to change.", 170, 170, 170, 255, this.sgSelf, this.changeColorSelf::get
   );
   public final Setting<SettingColor> colorThreeSelf = this.setting(
      "color-three-self", "The third confetti color to change.", 85, 85, 85, 255, this.sgSelf, this.changeColorSelf::get
   );
   public final Setting<SettingColor> colorFourSelf = this.setting(
      "color-four-self", "The fourth confetti color to change.", 0, 0, 0, 255, this.sgSelf, this.changeColorSelf::get
   );
   public final Setting<Boolean> selfExtra = this.setting(
      "extra-particles-self", "Spawns more or less particles on yourself.", Boolean.valueOf(true), this.sgSelf
   );
   public final Setting<Integer> extraAmountSelf = this.setting(
      "extra-self-particle-emitters-amount",
      "How many particle emitters to spawn on yourself.",
      Integer.valueOf(1),
      this.sgSelf,
      this.selfExtra::get,
      1.0,
      100.0
   );
   public final Setting<Integer> aliveTicksSelf = this.setting(
      "self-emitter-duration", "How many times the particle emitter will fire on yourself.", Integer.valueOf(30), this.sgSelf, this.selfExtra::get, 1.0, 150.0
   );
   public final Setting<Boolean> changeSizeSelf = this.setting("change-size-self", "Whether or not to change self size.", Boolean.valueOf(true), this.sgSelf);
   public final Setting<Double> sizeOneSelf = this.setting(
      "size-one-self", "The first confetti size.", Double.valueOf(1.0), this.sgSelf, this.changeSizeSelf::get, 0.0, 2.0
   );
   public final Setting<Double> sizeTwoSelf = this.setting(
      "size-two-self", "The second confetti size.", Double.valueOf(1.0), this.sgSelf, this.changeSizeSelf::get, 0.0, 2.0
   );
   public final Setting<Double> sizeThreeSelf = this.setting(
      "size-three-self", "The third confetti size.", Double.valueOf(1.0), this.sgSelf, this.changeSizeSelf::get, 0.0, 2.0
   );
   public final Setting<Double> sizeFourSelf = this.setting(
      "size-four-self", "The fourth confetti size.", Double.valueOf(1.0), this.sgSelf, this.changeSizeSelf::get, 0.0, 2.0
   );
   public final Setting<Boolean> changeColorOther = this.setting("change-color-other", "Whether or not to change color.", Boolean.valueOf(true));
   public final Setting<SettingColor> colorOneOther = this.setting(
      "color-one-other", "The first confetti color to change.", 255, 255, 255, 255, this.sgOther, this.changeColorOther::get
   );
   public final Setting<SettingColor> colorTwoOther = this.setting(
      "color-two-other", "The second confetti color to change.", 170, 170, 170, 255, this.sgOther, this.changeColorOther::get
   );
   public final Setting<SettingColor> colorThreeOther = this.setting(
      "color-three-other", "The third confetti color to change.", 85, 85, 85, 255, this.sgOther, this.changeColorOther::get
   );
   public final Setting<SettingColor> colorFourOther = this.setting(
      "color-four-other", "The fourth confetti color to change.", 0, 0, 0, 255, this.sgOther, this.changeColorOther::get
   );
   public final Setting<Boolean> otherExtra = this.setting(
      "extra-particles-other", "Spawns more or less particles on other entites.", Boolean.valueOf(true), this.sgOther
   );
   public final Setting<Integer> extraAmountOther = this.setting(
      "extra-other-particle-emitters-amount",
      "How many particle emitters to spawn on others.",
      Integer.valueOf(1),
      this.sgOther,
      this.otherExtra::get,
      1.0,
      100.0
   );
   public final Setting<Integer> aliveTicksOther = this.setting(
      "other-emitter-duration",
      "How many times the particle emitter will fire on others.",
      Integer.valueOf(30),
      this.sgOther,
      this.otherExtra::get,
      1.0,
      100.0
   );
   public final Setting<Boolean> changeSizeOther = this.setting(
      "change-size-other", "Whether or not to change other size.", Boolean.valueOf(true), this.sgOther
   );
   public final Setting<Double> sizeOneOther = this.setting(
      "size-one-other", "The first confetti size.", Double.valueOf(1.0), this.sgOther, this.changeSizeOther::get, 0.0, 2.0
   );
   public final Setting<Double> sizeTwoOther = this.setting(
      "size-two-other", "The second confetti size.", Double.valueOf(1.0), this.sgOther, this.changeSizeOther::get, 0.0, 2.0
   );
   public final Setting<Double> sizeThreeOther = this.setting(
      "size-three-other", "The third confetti size.", Double.valueOf(1.0), this.sgOther, this.changeSizeOther::get, 0.0, 2.0
   );
   public final Setting<Double> sizeFourOther = this.setting(
      "size-four-other", "The fourth confetti size.", Double.valueOf(1.0), this.sgOther, this.changeSizeOther::get, 0.0, 2.0
   );
   public boolean isSelf;

   public BetterPops() {
      super(Venomhack420.CATEGORY, "better-Pops", "Changes various aspects about totem pop particles.");
   }

   public Vec3d getDoubleVectorColor(Setting<SettingColor> colorSetting) {
      return new Vec3d(
         (double)((SettingColor)colorSetting.get()).r / 255.0,
         (double)((SettingColor)colorSetting.get()).g / 255.0,
         (double)((SettingColor)colorSetting.get()).b / 255.0
      );
   }

   public boolean shouldChangeSize(boolean self) {
      return this.changeSizeSelf.get() && self || this.changeSizeOther.get() && !self;
   }

   public boolean shouldSpawnExtra(boolean self) {
      return this.selfExtra.get() && self || this.otherExtra.get() && !self;
   }

   public boolean shouldChangeColor(boolean self) {
      return this.changeColorSelf.get() & self || this.changeColorOther.get() && !self;
   }

   @EventHandler
   private void onPacketRecieve(Receive event) {
      Packet entity = event.packet;
      if (entity instanceof EntityStatusS2CPacket) {
         EntityStatusS2CPacket p = (EntityStatusS2CPacket)entity;
         if (p.getStatus() == 35) {
            Entity entityx = p.getEntity(this.mc.world);
            if (entityx != null) {
               this.isSelf = entityx == this.mc.player
                  || this.friendsCopySelf.get() && entityx instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity)entityx);
            }
         }
      }
   }
}
